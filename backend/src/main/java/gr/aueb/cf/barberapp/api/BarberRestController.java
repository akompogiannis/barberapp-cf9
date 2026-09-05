package gr.aueb.cf.barberapp.api;

import gr.aueb.cf.barberapp.core.exceptions.EntityNotFoundException;
import gr.aueb.cf.barberapp.dto.BarberProfileDTO;
import gr.aueb.cf.barberapp.dto.ErrorResponseDTO;
import gr.aueb.cf.barberapp.service.IBarberProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/barber")
@RequiredArgsConstructor
@Tag(name = "Barber", description = "The public shop profile")
public class BarberRestController {

    private final IBarberProfileService barberProfileService;

    @Operation(
            summary = "Get the shop profile",
            description = """
                    Public. One call returns everything the landing page needs: who the barber
                    is, the service catalogue with promotional pricing applied, the promotions
                    running today, and the weekly opening hours.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile returned",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BarberProfileDTO.class))),
            @ApiResponse(responseCode = "404", description = "No barber has been set up",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/profile")
    public ResponseEntity<BarberProfileDTO> getProfile() throws EntityNotFoundException {
        return ResponseEntity.ok(barberProfileService.getProfile());
    }
}
