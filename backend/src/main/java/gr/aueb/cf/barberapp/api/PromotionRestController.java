package gr.aueb.cf.barberapp.api;

import gr.aueb.cf.barberapp.core.exceptions.EntityInvalidArgumentException;
import gr.aueb.cf.barberapp.core.exceptions.EntityNotFoundException;
import gr.aueb.cf.barberapp.core.exceptions.ValidationException;
import gr.aueb.cf.barberapp.dto.*;
import gr.aueb.cf.barberapp.service.IPromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
@Tag(name = "Promotions", description = "Seasonal discounts")
public class PromotionRestController {

    private final IPromotionService promotionService;

    @Operation(
            summary = "List the promotions running today",
            description = "Public. This is what the landing page advertises."
    )
    @ApiResponse(responseCode = "200", description = "Promotions returned")
    @GetMapping("/active")
    public ResponseEntity<List<PromotionReadOnlyDTO>> getActivePromotions() {
        return ResponseEntity.ok(promotionService.getActivePromotions());
    }

    @Operation(
            summary = "Browse all promotions",
            description = "Requires MANAGE_PROMOTIONS. Includes expired and switched-off ones."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponse(responseCode = "200", description = "Promotions returned")
    @GetMapping
    public ResponseEntity<Page<PromotionReadOnlyDTO>> getPromotions(
            @PageableDefault(page = 0, size = 10, sort = "validFrom") Pageable pageable) {
        return ResponseEntity.ok(promotionService.getPaginatedPromotions(pageable));
    }

    @Operation(summary = "Create a promotion", description = "Requires MANAGE_PROMOTIONS.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Promotion created",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PromotionReadOnlyDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation error, or the end date precedes the start",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ValidationErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "One of the services was not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping
    public ResponseEntity<PromotionReadOnlyDTO> createPromotion(
            @Valid @RequestBody PromotionInsertDTO dto,
            BindingResult bindingResult)
            throws EntityNotFoundException, EntityInvalidArgumentException, ValidationException {

        if (bindingResult.hasErrors()) {
            throw new ValidationException("Promotion", "Invalid promotion data", bindingResult);
        }

        PromotionReadOnlyDTO created = promotionService.savePromotion(dto);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{uuid}")
                .buildAndExpand(created.uuid())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @Operation(
            summary = "Update a promotion",
            description = """
                    Requires MANAGE_PROMOTIONS. The service list is replaced wholesale by the one
                    in the body. Appointments already booked keep the price they were quoted.
                    """
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Promotion updated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PromotionReadOnlyDTO.class))),
            @ApiResponse(responseCode = "404", description = "Promotion not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PutMapping("/{uuid}")
    public ResponseEntity<PromotionReadOnlyDTO> updatePromotion(
            @PathVariable UUID uuid,
            @Valid @RequestBody PromotionUpdateDTO dto,
            BindingResult bindingResult)
            throws EntityNotFoundException, EntityInvalidArgumentException, ValidationException {

        if (bindingResult.hasErrors()) {
            throw new ValidationException("Promotion", "Invalid promotion data", bindingResult);
        }
        if (!uuid.equals(dto.uuid())) {
            throw new ValidationException("Promotion", "The uuid in the path and the body must match", bindingResult);
        }

        return ResponseEntity.ok(promotionService.updatePromotion(dto));
    }

    @Operation(summary = "Withdraw a promotion", description = "Requires MANAGE_PROMOTIONS. Soft delete.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Promotion withdrawn"),
            @ApiResponse(responseCode = "404", description = "Promotion not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @DeleteMapping("/{uuid}")
    public ResponseEntity<PromotionReadOnlyDTO> deletePromotion(@PathVariable UUID uuid)
            throws EntityNotFoundException {
        return ResponseEntity.ok(promotionService.deletePromotion(uuid));
    }
}
