package gr.aueb.cf.barberapp.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record BarberServiceUpdateDTO(
        @NotNull(message = "Service uuid is required")
        java.util.UUID uuid,

        @NotBlank(message = "Service name is required")
        @Size(min = 2, max = 100)
        String name,

        @Size(max = 1000)
        String description,

        @NotNull(message = "Duration is required")
        @Min(value = 5, message = "Duration must be at least 5 minutes")
        @Max(value = 480, message = "Duration cannot exceed 8 hours")
        Integer durationMinutes,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", message = "Price cannot be negative")
        @Digits(integer = 8, fraction = 2)
        BigDecimal price,

        boolean active
) {}
