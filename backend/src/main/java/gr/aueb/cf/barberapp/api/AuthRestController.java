package gr.aueb.cf.barberapp.api;

import gr.aueb.cf.barberapp.authentication.AuthenticationService;
import gr.aueb.cf.barberapp.dto.AuthenticationRequestDTO;
import gr.aueb.cf.barberapp.dto.AuthenticationResponseDTO;
import gr.aueb.cf.barberapp.dto.ErrorResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Obtain a JWT")
public class AuthRestController {

    private final AuthenticationService authenticationService;

    @Operation(
            summary = "Authenticate and receive a JWT",
            description = """
                    Exchanges a username and password for a bearer token. Send the token as
                    `Authorization: Bearer <token>` on every subsequent call.

                    The response also carries the account's capabilities, which the front-end
                    uses to decide what to render.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthenticationResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Bad credentials or disabled account",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponseDTO> authenticate(
            @Valid @RequestBody AuthenticationRequestDTO dto) {

        return ResponseEntity.ok(authenticationService.authenticate(dto));
    }
}
