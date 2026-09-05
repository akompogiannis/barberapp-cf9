package gr.aueb.cf.barberapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserUpdateDTO(
        @NotBlank(message = "Firstname is required")
        @Size(min = 2, max = 100)
        String firstname,

        @NotBlank(message = "Lastname is required")
        @Size(min = 2, max = 100)
        String lastname,

        @NotBlank(message = "Email is required")
        @Email(message = "Must be a well-formed email address")
        String email,

        @Pattern(regexp = "^\\+?\\d{10,15}$",
                 message = "Phone must be 10 to 15 digits, optionally starting with +")
        String phone
) {}
