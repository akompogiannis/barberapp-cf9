package gr.aueb.cf.barberapp.dto;

import java.util.List;

public record BarberProfileDTO(
        String uuid,
        String shopName,
        String bio,
        String address,
        String phone,
        String photoUrl,
        List<BarberServiceReadOnlyDTO> services,
        List<PromotionReadOnlyDTO> activePromotions,
        List<WorkingHoursDTO> workingHours
) {}
