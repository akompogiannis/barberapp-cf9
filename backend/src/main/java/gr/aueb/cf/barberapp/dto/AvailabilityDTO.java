package gr.aueb.cf.barberapp.dto;

import java.time.LocalDate;
import java.util.List;

public record AvailabilityDTO(
        LocalDate date,
        String serviceUuid,
        String serviceName,
        Integer durationMinutes,
        boolean closed,
        List<TimeSlotDTO> slots
) {}
