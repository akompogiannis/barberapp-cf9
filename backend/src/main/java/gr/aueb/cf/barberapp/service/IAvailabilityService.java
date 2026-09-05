package gr.aueb.cf.barberapp.service;

import gr.aueb.cf.barberapp.core.SlotCalculator;
import gr.aueb.cf.barberapp.core.exceptions.EntityInvalidArgumentException;
import gr.aueb.cf.barberapp.core.exceptions.EntityNotFoundException;
import gr.aueb.cf.barberapp.core.exceptions.SlotUnavailableException;
import gr.aueb.cf.barberapp.dto.AvailabilityDTO;
import gr.aueb.cf.barberapp.model.BarberService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface IAvailabilityService {

    // Free slots for one service on one day.
    AvailabilityDTO getAvailability(UUID serviceUuid, LocalDate date)
            throws EntityNotFoundException, EntityInvalidArgumentException;

    // Rejects a requested start time that is not genuinely bookable: in the past, too far ahead, off
    // the slot grid, outside working hours, during time off, or overlapping an existing booking.
    void assertBookable(BarberService service, LocalDateTime startAt, UUID excludeUuid)
            throws SlotUnavailableException;

    // The working blocks that apply on a given date, or empty if closed.
    List<SlotCalculator.WorkingBlock> workingBlocksOn(Long barberId, LocalDate date);
}
