package gr.aueb.cf.barberapp.service;

import gr.aueb.cf.barberapp.core.exceptions.EntityInvalidArgumentException;
import gr.aueb.cf.barberapp.core.exceptions.EntityNotFoundException;
import gr.aueb.cf.barberapp.dto.TimeOffInsertDTO;
import gr.aueb.cf.barberapp.dto.TimeOffReadOnlyDTO;
import gr.aueb.cf.barberapp.dto.WorkingHoursDTO;
import gr.aueb.cf.barberapp.mapper.Mapper;
import gr.aueb.cf.barberapp.model.Barber;
import gr.aueb.cf.barberapp.model.TimeOff;
import gr.aueb.cf.barberapp.model.WorkingHours;
import gr.aueb.cf.barberapp.repository.BarberRepository;
import gr.aueb.cf.barberapp.repository.TimeOffRepository;
import gr.aueb.cf.barberapp.repository.WorkingHoursRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service                        // IoC Container
@RequiredArgsConstructor        // DI
@Slf4j                          // Logger
public class ScheduleServiceImpl implements IScheduleService {

    private final WorkingHoursRepository workingHoursRepository;
    private final TimeOffRepository timeOffRepository;
    private final BarberRepository barberRepository;
    private final Mapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<WorkingHoursDTO> getWorkingHours() {
        Barber barber = barberRepository.findFirstByDeletedFalseOrderByIdAsc().orElse(null);
        if (barber == null) return List.of();

        return workingHoursRepository
                .findAllByBarber_IdAndDeletedFalseOrderByDayOfWeekAscStartTimeAsc(barber.getId())
                .stream()
                .map(mapper::mapToWorkingHoursDTO)
                .toList();
    }

    @Override
    @PreAuthorize("hasAuthority('MANAGE_SCHEDULE')")
    @Transactional(rollbackFor = EntityInvalidArgumentException.class)
    public List<WorkingHoursDTO> replaceWorkingHours(List<WorkingHoursDTO> blocks)
            throws EntityInvalidArgumentException {

        Barber barber = barberRepository.findFirstByDeletedFalseOrderByIdAsc()
                .orElseThrow(() -> new EntityInvalidArgumentException("Barber",
                        "No barber profile exists - check the seed migration"));

        validateBlocks(blocks);

        // A hard delete is right here: working hours are a template, not history.
        // Appointments already booked recorded their own times and are untouched.
        List<WorkingHours> existing = workingHoursRepository
                .findAllByBarber_IdAndDeletedFalseOrderByDayOfWeekAscStartTimeAsc(barber.getId());
        workingHoursRepository.deleteAll(existing);
        workingHoursRepository.flush();     // release the unique key before reinserting

        List<WorkingHours> saved = new ArrayList<>();
        for (WorkingHoursDTO dto : blocks) {
            WorkingHours hours = new WorkingHours();
            hours.setDayOfWeek(dto.dayOfWeek());
            hours.setStartTime(dto.startTime());
            hours.setEndTime(dto.endTime());
            hours.setBarber(barber);
            saved.add(workingHoursRepository.save(hours));
        }

        log.info("Working hours replaced with {} blocks", saved.size());
        return saved.stream().map(mapper::mapToWorkingHoursDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeOffReadOnlyDTO> getTimeOff(LocalDate from, LocalDate to) {
        Barber barber = barberRepository.findFirstByDeletedFalseOrderByIdAsc().orElse(null);
        if (barber == null) return List.of();

        LocalDate start = from != null ? from : LocalDate.MIN;
        LocalDate end = to != null ? to : LocalDate.MAX;

        return timeOffRepository
                .findOverlapping(barber.getId(), start.atStartOfDay(), end.plusDays(1).atStartOfDay())
                .stream()
                .map(mapper::mapToTimeOffReadOnlyDTO)
                .toList();
    }

    @Override
    @PreAuthorize("hasAuthority('MANAGE_SCHEDULE')")
    @Transactional(rollbackFor = EntityInvalidArgumentException.class)
    public TimeOffReadOnlyDTO addTimeOff(TimeOffInsertDTO dto) throws EntityInvalidArgumentException {

        if (!dto.startAt().isBefore(dto.endAt())) {
            throw new EntityInvalidArgumentException("TimeOff", "The end must come after the start");
        }

        Barber barber = barberRepository.findFirstByDeletedFalseOrderByIdAsc()
                .orElseThrow(() -> new EntityInvalidArgumentException("Barber",
                        "No barber profile exists - check the seed migration"));

        TimeOff timeOff = new TimeOff();
        timeOff.setStartAt(dto.startAt());
        timeOff.setEndAt(dto.endAt());
        timeOff.setReason(dto.reason());
        barber.addTimeOff(timeOff);

        timeOffRepository.save(timeOff);

        // Deliberately does not cancel appointments that now fall inside the block.
        log.info("Time off registered from {} to {}", dto.startAt(), dto.endAt());
        return mapper.mapToTimeOffReadOnlyDTO(timeOff);
    }

    @Override
    @PreAuthorize("hasAuthority('MANAGE_SCHEDULE')")
    @Transactional(rollbackFor = EntityNotFoundException.class)
    public TimeOffReadOnlyDTO deleteTimeOff(UUID uuid) throws EntityNotFoundException {
        TimeOff timeOff = timeOffRepository.findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() -> new EntityNotFoundException("TimeOff",
                        "Time off with uuid=" + uuid + " not found"));

        timeOff.softDelete();
        log.info("Time off uuid={} removed", uuid);
        return mapper.mapToTimeOffReadOnlyDTO(timeOff);
    }

    // Each block must be well formed, and blocks on the same day must not overlap -
    // overlapping blocks would make the same slot be offered twice.
    private void validateBlocks(List<WorkingHoursDTO> blocks) throws EntityInvalidArgumentException {
        if (blocks == null || blocks.isEmpty()) return;

        for (WorkingHoursDTO block : blocks) {
            if (!block.startTime().isBefore(block.endTime())) {
                throw new EntityInvalidArgumentException("WorkingHours",
                        block.dayOfWeek() + ": the end time must come after the start time");
            }
        }

        List<WorkingHoursDTO> sorted = blocks.stream()
                .sorted(Comparator.comparing(WorkingHoursDTO::dayOfWeek)
                        .thenComparing(WorkingHoursDTO::startTime))
                .toList();

        for (int i = 1; i < sorted.size(); i++) {
            WorkingHoursDTO previous = sorted.get(i - 1);
            WorkingHoursDTO current = sorted.get(i);
            if (previous.dayOfWeek() == current.dayOfWeek()
                    && current.startTime().isBefore(previous.endTime())) {
                throw new EntityInvalidArgumentException("WorkingHours",
                        current.dayOfWeek() + ": blocks " + previous.startTime() + "-" + previous.endTime()
                                + " and " + current.startTime() + "-" + current.endTime() + " overlap");
            }
        }
    }
}
