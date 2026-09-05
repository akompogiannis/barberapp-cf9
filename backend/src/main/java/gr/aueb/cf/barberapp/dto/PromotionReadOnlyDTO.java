package gr.aueb.cf.barberapp.dto;

import java.time.LocalDate;
import java.util.List;

public record PromotionReadOnlyDTO(
        String uuid,
        String title,
        String description,
        Integer discountPercent,
        LocalDate validFrom,
        LocalDate validTo,
        boolean active,
        List<String> serviceNames
) {}
