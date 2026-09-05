package gr.aueb.cf.barberapp.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record TimeOffInsertDTO(
        @NotNull(message = "Start is required")
        LocalDateTime startAt,

        @NotNull(message = "End is required")
        LocalDateTime endAt,

        @Size(max = 255)
        String reason
) {}
