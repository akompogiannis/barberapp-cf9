package gr.aueb.cf.barberapp.api;

import gr.aueb.cf.barberapp.core.exceptions.*;
import gr.aueb.cf.barberapp.dto.ErrorResponseDTO;
import gr.aueb.cf.barberapp.dto.UserInsertDTO;
import gr.aueb.cf.barberapp.dto.UserReadOnlyDTO;
import gr.aueb.cf.barberapp.dto.UserUpdateDTO;
import gr.aueb.cf.barberapp.dto.ValidationErrorResponseDTO;
import gr.aueb.cf.barberapp.service.IUserService;
import gr.aueb.cf.barberapp.validator.UserInsertValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Registration and profile")
public class UserRestController {

    private final IUserService userService;
    private final UserInsertValidator userInsertValidator;

    @Operation(
            summary = "Register a new customer account",
            description = "Public sign-up. The account is always created with the CUSTOMER role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserReadOnlyDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ValidationErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Username or email already taken",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<UserReadOnlyDTO> register(@Valid @RequestBody UserInsertDTO dto,
                                                    BindingResult bindingResult)
            throws EntityAlreadyExistsException, EntityInvalidArgumentException, ValidationException {

        userInsertValidator.validate(dto, bindingResult);
        if (bindingResult.hasErrors()) {
            throw new ValidationException("User", "Invalid registration data", bindingResult);
        }

        UserReadOnlyDTO created = userService.register(dto);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Get the signed-in account")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account returned",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserReadOnlyDTO.class)))
    })
    @GetMapping("/me")
    public ResponseEntity<UserReadOnlyDTO> me(@AuthenticationPrincipal UserDetails principal)
            throws EntityNotFoundException {

        return ResponseEntity.ok(userService.getByUsername(principal.getUsername()));
    }

    @Operation(
            summary = "Update the signed-in account",
            description = "Only the caller's own profile can be edited; the uuid is taken from the token."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserReadOnlyDTO.class))),
            @ApiResponse(responseCode = "409", description = "Email already registered",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PutMapping("/me")
    public ResponseEntity<UserReadOnlyDTO> updateMe(@AuthenticationPrincipal UserDetails principal,
                                                    @Valid @RequestBody UserUpdateDTO dto,
                                                    BindingResult bindingResult)
            throws EntityNotFoundException, EntityAlreadyExistsException, ValidationException {

        if (bindingResult.hasErrors()) {
            throw new ValidationException("User", "Invalid profile data", bindingResult);
        }

        return ResponseEntity.ok(userService.updateOwnProfile(principal.getUsername(), dto));
    }
}
