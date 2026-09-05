package gr.aueb.cf.barberapp.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

// Note there is no price field: the server prices the booking from the service
// and any live promotion, so a client cannot dictate what it pays.
public record AppointmentInsertDTO(
        @NotNull(message = "Service uuid is required")
        UUID serviceUuid,

        @NotNull(message = "Start time is required")
        LocalDateTime startAt,

        @Size(max = 1000)
        String notes
) {}
