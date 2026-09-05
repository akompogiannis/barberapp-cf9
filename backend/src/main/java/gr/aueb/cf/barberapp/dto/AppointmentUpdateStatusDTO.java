package gr.aueb.cf.barberapp.dto;

import gr.aueb.cf.barberapp.model.AppointmentStatus;
import jakarta.validation.constraints.NotNull;

public record AppointmentUpdateStatusDTO(
        @NotNull(message = "Status is required")
        AppointmentStatus status
) {}
