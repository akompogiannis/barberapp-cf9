package gr.aueb.cf.barberapp.dto;

import java.time.LocalDateTime;

public record TimeOffReadOnlyDTO(
        String uuid,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String reason
) {}
