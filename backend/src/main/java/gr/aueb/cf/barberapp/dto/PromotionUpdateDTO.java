package gr.aueb.cf.barberapp.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PromotionUpdateDTO(
        @NotNull(message = "Promotion uuid is required")
        UUID uuid,

        @NotBlank(message = "Title is required")
        @Size(min = 3, max = 255)
        String title,

        @Size(max = 1000)
        String description,

        @NotNull(message = "Discount is required")
        @Min(value = 1, message = "Discount must be at least 1%")
        @Max(value = 100, message = "Discount cannot exceed 100%")
        Integer discountPercent,

        @NotNull(message = "Start date is required")
        LocalDate validFrom,

        @NotNull(message = "End date is required")
        LocalDate validTo,

        boolean active,

        @NotEmpty(message = "A promotion must apply to at least one service")
        List<UUID> serviceUuids
) {}
