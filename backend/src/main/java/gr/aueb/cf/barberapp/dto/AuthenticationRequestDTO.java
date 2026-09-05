package gr.aueb.cf.barberapp.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthenticationRequestDTO(
        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Password is required")
        String password
) {}
