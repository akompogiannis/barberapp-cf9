package gr.aueb.cf.barberapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Public self-registration. Always creates a CUSTOMER - the role is not client-supplied.
public record UserInsertDTO(
        @NotBlank(message = "Username is required")
        @Size(min = 4, max = 50, message = "Username must be between 4 and 50 characters")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).*$",
                 message = "Password must contain at least one letter and one digit")
        String password,

        @NotBlank(message = "Password confirmation is required")
        String confirmPassword,

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
