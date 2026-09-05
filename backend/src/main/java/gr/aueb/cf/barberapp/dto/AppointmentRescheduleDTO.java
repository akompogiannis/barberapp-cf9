package gr.aueb.cf.barberapp.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AppointmentRescheduleDTO(
        @NotNull(message = "New start time is required")
        LocalDateTime startAt
) {}
