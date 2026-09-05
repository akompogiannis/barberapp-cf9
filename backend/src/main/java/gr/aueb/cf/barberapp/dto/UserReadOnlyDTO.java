package gr.aueb.cf.barberapp.dto;

import java.util.Set;

public record UserReadOnlyDTO(
        String uuid,
        String username,
        String firstname,
        String lastname,
        String email,
        String phone,
        String role,
        Set<String> capabilities,
        String customerUuid
) {}
