package gr.aueb.cf.barberapp.dto;

import java.math.BigDecimal;

public record BarberServiceReadOnlyDTO(
        String uuid,
        String name,
        String description,
        Integer durationMinutes,
        BigDecimal price,
        BigDecimal promotionalPrice,
        String promotionTitle,
        boolean active
) {}
