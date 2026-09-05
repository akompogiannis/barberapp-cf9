package gr.aueb.cf.barberapp.service;

import gr.aueb.cf.barberapp.core.exceptions.EntityInvalidArgumentException;
import gr.aueb.cf.barberapp.core.exceptions.EntityNotFoundException;
import gr.aueb.cf.barberapp.core.exceptions.SlotUnavailableException;
import gr.aueb.cf.barberapp.core.filters.AppointmentFilters;
import gr.aueb.cf.barberapp.dto.AppointmentInsertDTO;
import gr.aueb.cf.barberapp.dto.AppointmentReadOnlyDTO;
import gr.aueb.cf.barberapp.model.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

public interface IAppointmentService {

    // Books a slot for the authenticated customer.
    AppointmentReadOnlyDTO book(String username, AppointmentInsertDTO dto)
            throws EntityNotFoundException, SlotUnavailableException, EntityInvalidArgumentException;

    // The barber's diary, filtered and paginated.
    Page<AppointmentReadOnlyDTO> getAppointments(Pageable pageable, AppointmentFilters filters);

    // The signed-in customer's own bookings.
    Page<AppointmentReadOnlyDTO> getOwnAppointments(String username, Pageable pageable)
            throws EntityNotFoundException;

    AppointmentReadOnlyDTO getByUuid(UUID uuid) throws EntityNotFoundException;

    // Barber-only lifecycle move: confirm, complete, cancel or mark a no-show.
    AppointmentReadOnlyDTO changeStatus(UUID uuid, AppointmentStatus target)
            throws EntityNotFoundException, EntityInvalidArgumentException;

    // Moves a booking to a different time, re-running every availability check.
    AppointmentReadOnlyDTO reschedule(UUID uuid, LocalDateTime newStart)
            throws EntityNotFoundException, SlotUnavailableException, EntityInvalidArgumentException;

    // Cancellation. Reachable by the barber, or by the customer who owns it.
    AppointmentReadOnlyDTO cancel(UUID uuid) throws EntityNotFoundException, EntityInvalidArgumentException;
}
