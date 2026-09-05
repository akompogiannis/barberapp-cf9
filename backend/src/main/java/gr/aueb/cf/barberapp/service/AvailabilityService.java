package gr.aueb.cf.barberapp.service;

import gr.aueb.cf.barberapp.core.SlotCalculator;
import gr.aueb.cf.barberapp.core.exceptions.EntityInvalidArgumentException;
import gr.aueb.cf.barberapp.core.exceptions.EntityNotFoundException;
import gr.aueb.cf.barberapp.core.exceptions.SlotUnavailableException;
import gr.aueb.cf.barberapp.dto.AvailabilityDTO;
import gr.aueb.cf.barberapp.dto.TimeSlotDTO;
import gr.aueb.cf.barberapp.model.Appointment;
import gr.aueb.cf.barberapp.model.AppointmentStatus;
import gr.aueb.cf.barberapp.model.BarberService;
import gr.aueb.cf.barberapp.model.TimeOff;
import gr.aueb.cf.barberapp.repository.AppointmentRepository;
import gr.aueb.cf.barberapp.repository.BarberServiceRepository;
import gr.aueb.cf.barberapp.repository.TimeOffRepository;
import gr.aueb.cf.barberapp.repository.WorkingHoursRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

// Availability is always calculated, never stored. Working hours + time off + the appointments
// already booked go in, free slots come out.
@Service                        // IoC Container
@RequiredArgsConstructor        // DI
@Slf4j                          // Logger
public class AvailabilityService implements IAvailabilityService {

    // statuses that still take up time in the diary
    private static final List<AppointmentStatus> BLOCKING_STATUSES =
            Arrays.stream(AppointmentStatus.values())
                    .filter(AppointmentStatus::blocksSlot)
                    .toList();

    private final BarberServiceRepository barberServiceRepository;
    private final WorkingHoursRepository workingHoursRepository;
    private final TimeOffRepository timeOffRepository;
    private final AppointmentRepository appointmentRepository;
    private final Clock clock;

    @Value("${app.booking.max-days-ahead}")
    private int maxDaysAhead;

    @Override
    @Transactional(readOnly = true)
    public AvailabilityDTO getAvailability(UUID serviceUuid, LocalDate date)
            throws EntityNotFoundException, EntityInvalidArgumentException {

        if (date == null) {
            throw new EntityInvalidArgumentException("Date", "A date is required");
        }

        LocalDate today = LocalDate.now(clock);
        if (date.isBefore(today)) {
            throw new EntityInvalidArgumentException("Date", "Cannot query availability in the past");
        }
        if (date.isAfter(today.plusDays(maxDaysAhead))) {
            throw new EntityInvalidArgumentException("Date",
                    "Cannot query availability more than " + maxDaysAhead + " days ahead");
        }

        BarberService service = barberServiceRepository.findByUuidAndDeletedFalse(serviceUuid)
                .orElseThrow(() -> new EntityNotFoundException("Service",
                        "Service with uuid=" + serviceUuid + " not found"));

        if (!service.isActive()) {
            throw new EntityInvalidArgumentException("Service",
                    "Service " + service.getName() + " is not currently offered");
        }

        Long barberId = service.getBarber().getId();
        List<SlotCalculator.WorkingBlock> blocks = workingBlocksOn(barberId, date);

        if (blocks.isEmpty()) {
            log.debug("No working hours for barberId={} on {} - shop closed", barberId, date);
            return new AvailabilityDTO(date, service.getUuid().toString(), service.getName(),
                    service.getDurationMinutes(), true, List.of());
        }

        List<SlotCalculator.BusyInterval> busy = busyIntervalsOn(barberId, date);

        List<TimeSlotDTO> slots = SlotCalculator.freeSlots(
                date,
                blocks,
                busy,
                service.getDurationMinutes(),
                service.getBarber().getSlotStepMinutes(),
                LocalDateTime.now(clock)
        );

        log.debug("Availability for service={} on {} returned {} slots", service.getName(), date, slots.size());

        return new AvailabilityDTO(date, service.getUuid().toString(), service.getName(),
                service.getDurationMinutes(), false, slots);
    }

    @Override
    @Transactional(readOnly = true)
    public void assertBookable(BarberService service, LocalDateTime startAt, UUID excludeUuid)
            throws SlotUnavailableException {

        LocalDateTime now = LocalDateTime.now(clock);

        if (startAt.isBefore(now)) {
            throw new SlotUnavailableException("Appointment", "Cannot book a time in the past");
        }
        if (startAt.toLocalDate().isAfter(LocalDate.now(clock).plusDays(maxDaysAhead))) {
            throw new SlotUnavailableException("Appointment",
                    "Cannot book more than " + maxDaysAhead + " days ahead");
        }

        LocalDate date = startAt.toLocalDate();
        LocalDateTime endAt = startAt.plusMinutes(service.getDurationMinutes());
        Long barberId = service.getBarber().getId();
        int step = service.getBarber().getSlotStepMinutes();

        List<SlotCalculator.WorkingBlock> blocks = workingBlocksOn(barberId, date);
        if (blocks.isEmpty()) {
            throw new SlotUnavailableException("Appointment", "The shop is closed on " + date);
        }
        if (!SlotCalculator.isOnSlotGrid(blocks, startAt.toLocalTime(), step)) {
            throw new SlotUnavailableException("Appointment",
                    "Appointments start every " + step + " minutes; " + startAt.toLocalTime() + " is not an offered time");
        }
        if (!SlotCalculator.isWithinWorkingHours(date, blocks, startAt, endAt)) {
            throw new SlotUnavailableException("Appointment",
                    "A " + service.getDurationMinutes() + " minute appointment starting at "
                            + startAt.toLocalTime() + " does not fit inside working hours");
        }

        // Time off is checked separately from appointments so the customer gets a
        // message that tells them which of the two it was.
        boolean duringTimeOff = timeOffRepository.findOverlapping(barberId, startAt, endAt).stream()
                .anyMatch(t -> t.overlaps(startAt, endAt));
        if (duringTimeOff) {
            throw new SlotUnavailableException("Appointment", "The barber is unavailable at that time");
        }

        // Runs inside the caller's transaction, immediately before the insert.
        boolean taken = appointmentRepository.existsBlockingOverlap(
                startAt, endAt, BLOCKING_STATUSES, excludeUuid);
        if (taken) {
            throw new SlotUnavailableException("Appointment", "That slot has just been taken");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SlotCalculator.WorkingBlock> workingBlocksOn(Long barberId, LocalDate date) {
        return workingHoursRepository
                .findAllByBarber_IdAndDayOfWeekAndDeletedFalseOrderByStartTimeAsc(barberId, date.getDayOfWeek())
                .stream()
                .map(wh -> new SlotCalculator.WorkingBlock(wh.getStartTime(), wh.getEndTime()))
                .toList();
    }

    // everything that already takes up time on that date - bookings and time off
    private List<SlotCalculator.BusyInterval> busyIntervalsOn(Long barberId, LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

        List<SlotCalculator.BusyInterval> fromAppointments = appointmentRepository
                .findBlockingBetween(dayStart, dayEnd, BLOCKING_STATUSES)
                .stream()
                .map(this::toInterval)
                .toList();

        List<SlotCalculator.BusyInterval> fromTimeOff = timeOffRepository
                .findOverlapping(barberId, dayStart, dayEnd)
                .stream()
                .map(this::toInterval)
                .toList();

        return java.util.stream.Stream.concat(fromAppointments.stream(), fromTimeOff.stream()).toList();
    }

    private SlotCalculator.BusyInterval toInterval(Appointment a) {
        return new SlotCalculator.BusyInterval(a.getStartAt(), a.getEndAt());
    }

    private SlotCalculator.BusyInterval toInterval(TimeOff t) {
        return new SlotCalculator.BusyInterval(t.getStartAt(), t.getEndAt());
    }
}
