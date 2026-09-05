package gr.aueb.cf.barberapp.service;

import gr.aueb.cf.barberapp.core.exceptions.EntityInvalidArgumentException;
import gr.aueb.cf.barberapp.core.exceptions.EntityNotFoundException;
import gr.aueb.cf.barberapp.core.exceptions.SlotUnavailableException;
import gr.aueb.cf.barberapp.core.filters.AppointmentFilters;
import gr.aueb.cf.barberapp.dto.AppointmentInsertDTO;
import gr.aueb.cf.barberapp.dto.AppointmentReadOnlyDTO;
import gr.aueb.cf.barberapp.mapper.Mapper;
import gr.aueb.cf.barberapp.model.*;
import gr.aueb.cf.barberapp.repository.AppointmentRepository;
import gr.aueb.cf.barberapp.repository.BarberServiceRepository;
import gr.aueb.cf.barberapp.repository.CustomerRepository;
import gr.aueb.cf.barberapp.repository.PromotionRepository;
import gr.aueb.cf.barberapp.specification.AppointmentSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service                        // IoC Container
@RequiredArgsConstructor        // DI
@Slf4j                          // Logger
public class AppointmentServiceImpl implements IAppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final BarberServiceRepository barberServiceRepository;
    private final CustomerRepository customerRepository;
    private final PromotionRepository promotionRepository;
    private final IAvailabilityService availabilityService;
    private final Mapper mapper;

    // One transaction on purpose - assertBookable re-checks for an overlap on the same
    // connection as the insert below, so a race for the last slot gives the loser a 409.
    @Override
    @PreAuthorize("hasAuthority('BOOK_APPOINTMENT')")
    @Transactional(rollbackFor = {EntityNotFoundException.class, SlotUnavailableException.class,
            EntityInvalidArgumentException.class})
    public AppointmentReadOnlyDTO book(String username, AppointmentInsertDTO dto)
            throws EntityNotFoundException, SlotUnavailableException, EntityInvalidArgumentException {

        try {
            Customer customer = customerRepository.findByUser_Username(username)
                    .orElseThrow(() -> new EntityNotFoundException("Customer",
                            "No customer profile for username=" + username));

            BarberService service = barberServiceRepository.findByUuidAndDeletedFalse(dto.serviceUuid())
                    .orElseThrow(() -> new EntityNotFoundException("Service",
                            "Service with uuid=" + dto.serviceUuid() + " not found"));

            if (!service.isActive()) {
                throw new EntityInvalidArgumentException("Service",
                        "Service " + service.getName() + " is not currently offered");
            }

            availabilityService.assertBookable(service, dto.startAt(), null);

            Appointment appointment = new Appointment();
            appointment.setBarberService(service);
            appointment.setStartAt(dto.startAt());
            appointment.setEndAt(dto.startAt().plusMinutes(service.getDurationMinutes()));
            appointment.setStatus(AppointmentStatus.PENDING);
            appointment.setNotes(dto.notes());
            customer.addAppointment(appointment);

            // Freeze the price. A promotion expiring tomorrow must not silently
            // reprice a booking that was made while it was live.
            Promotion promotion = bestPromotionFor(service, dto.startAt().toLocalDate());
            appointment.setPromotion(promotion);
            appointment.setPriceCharged(promotion != null
                    ? promotion.applyTo(service.getPrice())
                    : service.getPrice());

            appointmentRepository.save(appointment);

            log.info("Appointment uuid={} booked by username={} for service={} at {}",
                    appointment.getUuid(), username, service.getName(), dto.startAt());
            return mapper.mapToAppointmentReadOnlyDTO(appointment);

        } catch (SlotUnavailableException e) {
            log.warn("Booking rejected for username={} at {}: {}", username, dto.startAt(), e.getMessage());
            throw e;
        } catch (EntityNotFoundException e) {
            log.warn("Booking failed for username={}: {}", username, e.getMessage());
            throw e;
        }
    }

    @Override
    @PreAuthorize("hasAuthority('VIEW_APPOINTMENTS')")
    @Transactional(readOnly = true)
    public Page<AppointmentReadOnlyDTO> getAppointments(Pageable pageable, AppointmentFilters filters) {
        Page<AppointmentReadOnlyDTO> page = appointmentRepository
                .findAll(AppointmentSpecification.build(filters), pageable)
                .map(mapper::mapToAppointmentReadOnlyDTO);

        log.debug("Diary query returned page={} size={} total={}",
                page.getNumber(), page.getSize(), page.getTotalElements());
        return page;
    }

    @Override
    @PreAuthorize("hasAuthority('VIEW_OWN_APPOINTMENTS')")
    @Transactional(readOnly = true)
    public Page<AppointmentReadOnlyDTO> getOwnAppointments(String username, Pageable pageable)
            throws EntityNotFoundException {

        Customer customer = customerRepository.findByUser_Username(username)
                .orElseThrow(() -> new EntityNotFoundException("Customer",
                        "No customer profile for username=" + username));

        return appointmentRepository
                .findAllByCustomer_UuidAndDeletedFalse(customer.getUuid(), pageable)
                .map(mapper::mapToAppointmentReadOnlyDTO);
    }

    // The barber, or the customer it belongs to. A guessed uuid gets a 403.
    @Override
    @PreAuthorize("hasAuthority('VIEW_APPOINTMENTS') or @securityService.isOwnAppointment(#uuid, authentication)")
    @Transactional(readOnly = true)
    public AppointmentReadOnlyDTO getByUuid(UUID uuid) throws EntityNotFoundException {
        Appointment appointment = appointmentRepository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Appointment",
                        "Appointment with uuid=" + uuid + " not found"));
        return mapper.mapToAppointmentReadOnlyDTO(appointment);
    }

    @Override
    @PreAuthorize("hasAuthority('EDIT_APPOINTMENT')")
    @Transactional(rollbackFor = {EntityNotFoundException.class, EntityInvalidArgumentException.class})
    public AppointmentReadOnlyDTO changeStatus(UUID uuid, AppointmentStatus target)
            throws EntityNotFoundException, EntityInvalidArgumentException {

        Appointment appointment = appointmentRepository.findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Appointment",
                        "Appointment with uuid=" + uuid + " not found"));

        AppointmentStatus current = appointment.getStatus();
        if (!current.canTransitionTo(target)) {
            throw new EntityInvalidArgumentException("Appointment",
                    "Cannot move an appointment from " + current + " to " + target);
        }

        appointment.setStatus(target);
        log.info("Appointment uuid={} moved from {} to {}", uuid, current, target);
        return mapper.mapToAppointmentReadOnlyDTO(appointment);
    }

    @Override
    @PreAuthorize("hasAuthority('EDIT_APPOINTMENT') or @securityService.isOwnAppointment(#uuid, authentication)")
    @Transactional(rollbackFor = {EntityNotFoundException.class, SlotUnavailableException.class,
            EntityInvalidArgumentException.class})
    public AppointmentReadOnlyDTO reschedule(UUID uuid, LocalDateTime newStart)
            throws EntityNotFoundException, SlotUnavailableException, EntityInvalidArgumentException {

        Appointment appointment = appointmentRepository.findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Appointment",
                        "Appointment with uuid=" + uuid + " not found"));

        if (!appointment.getStatus().blocksSlot()) {
            throw new EntityInvalidArgumentException("Appointment",
                    "A " + appointment.getStatus() + " appointment cannot be rescheduled");
        }

        BarberService service = appointment.getBarberService();

        // Excluding this appointment from the overlap check, otherwise it would
        // always collide with the slot it currently occupies.
        availabilityService.assertBookable(service, newStart, appointment.getUuid());

        LocalDateTime previousStart = appointment.getStartAt();
        appointment.setStartAt(newStart);
        appointment.setEndAt(newStart.plusMinutes(service.getDurationMinutes()));

        log.info("Appointment uuid={} rescheduled from {} to {}", uuid, previousStart, newStart);
        return mapper.mapToAppointmentReadOnlyDTO(appointment);
    }

    @Override
    @PreAuthorize("hasAuthority('CANCEL_APPOINTMENT') or "
            + "(hasAuthority('CANCEL_OWN_APPOINTMENT') and @securityService.isOwnAppointment(#uuid, authentication))")
    @Transactional(rollbackFor = {EntityNotFoundException.class, EntityInvalidArgumentException.class})
    public AppointmentReadOnlyDTO cancel(UUID uuid)
            throws EntityNotFoundException, EntityInvalidArgumentException {

        Appointment appointment = appointmentRepository.findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Appointment",
                        "Appointment with uuid=" + uuid + " not found"));

        if (!appointment.getStatus().canTransitionTo(AppointmentStatus.CANCELLED)) {
            throw new EntityInvalidArgumentException("Appointment",
                    "A " + appointment.getStatus() + " appointment cannot be cancelled");
        }

        // Cancelled, not soft-deleted: the row stays visible in the diary and the slot is freed because
        // CANCELLED does not block.
        appointment.setStatus(AppointmentStatus.CANCELLED);

        log.info("Appointment uuid={} cancelled", uuid);
        return mapper.mapToAppointmentReadOnlyDTO(appointment);
    }

    private Promotion bestPromotionFor(BarberService service, LocalDate date) {
        List<Promotion> applicable = promotionRepository.findApplicable(service.getId(), date);
        return applicable.isEmpty() ? null : applicable.get(0);
    }
}
