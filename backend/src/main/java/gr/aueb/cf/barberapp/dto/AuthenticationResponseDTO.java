package gr.aueb.cf.barberapp.dto;

public record AuthenticationResponseDTO(
        String token,
        long expiresInMillis,
        UserReadOnlyDTO user
) {}
