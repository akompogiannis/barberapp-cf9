package gr.aueb.cf.barberapp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AppointmentReadOnlyDTO(
        String uuid,
        String customerUuid,
        String customerName,
        String customerPhone,
        String serviceUuid,
        String serviceName,
        Integer durationMinutes,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String status,
        BigDecimal priceCharged,
        String promotionTitle,
        String notes
) {}
