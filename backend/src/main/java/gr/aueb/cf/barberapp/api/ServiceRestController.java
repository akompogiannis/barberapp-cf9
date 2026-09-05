package gr.aueb.cf.barberapp.api;

import gr.aueb.cf.barberapp.core.exceptions.*;
import gr.aueb.cf.barberapp.dto.*;
import gr.aueb.cf.barberapp.service.IServiceCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
@Tag(name = "Services", description = "The catalogue of bookable services")
public class ServiceRestController {

    private final IServiceCatalogService serviceCatalogService;

    @Operation(
            summary = "List the services on offer",
            description = """
                    Public. Returns active services only, already priced against any promotion
                    that is live today - `promotionalPrice` is null when nothing applies.
                    """
    )
    @ApiResponse(responseCode = "200", description = "Catalogue returned")
    @GetMapping
    public ResponseEntity<List<BarberServiceReadOnlyDTO>> getServices() {
        return ResponseEntity.ok(serviceCatalogService.getActiveServices());
    }

    @Operation(summary = "Get one service by uuid", description = "Public.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Service returned",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BarberServiceReadOnlyDTO.class))),
            @ApiResponse(responseCode = "404", description = "Service not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{uuid}")
    public ResponseEntity<BarberServiceReadOnlyDTO> getService(@PathVariable UUID uuid)
            throws EntityNotFoundException {
        return ResponseEntity.ok(serviceCatalogService.getServiceByUuid(uuid));
    }

    @Operation(summary = "Create a service", description = "Requires MANAGE_SERVICES.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Service created",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BarberServiceReadOnlyDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ValidationErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "A service with that name already exists",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping
    public ResponseEntity<BarberServiceReadOnlyDTO> createService(
            @Valid @RequestBody BarberServiceInsertDTO dto,
            BindingResult bindingResult)
            throws EntityAlreadyExistsException, EntityInvalidArgumentException, ValidationException {

        if (bindingResult.hasErrors()) {
            throw new ValidationException("Service", "Invalid service data", bindingResult);
        }

        BarberServiceReadOnlyDTO created = serviceCatalogService.saveService(dto);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{uuid}")
                .buildAndExpand(created.uuid())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "Update a service", description = "Requires MANAGE_SERVICES.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Service updated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BarberServiceReadOnlyDTO.class))),
            @ApiResponse(responseCode = "404", description = "Service not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PutMapping("/{uuid}")
    public ResponseEntity<BarberServiceReadOnlyDTO> updateService(
            @PathVariable UUID uuid,
            @Valid @RequestBody BarberServiceUpdateDTO dto,
            BindingResult bindingResult)
            throws EntityNotFoundException, EntityAlreadyExistsException, ValidationException {

        if (bindingResult.hasErrors()) {
            throw new ValidationException("Service", "Invalid service data", bindingResult);
        }
        if (!uuid.equals(dto.uuid())) {
            throw new ValidationException("Service", "The uuid in the path and the body must match", bindingResult);
        }

        return ResponseEntity.ok(serviceCatalogService.updateService(dto));
    }

    @Operation(
            summary = "Retire a service",
            description = """
                    Requires MANAGE_SERVICES. A soft delete: the service stops being offered but
                    appointments that already reference it keep working.
                    """
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Service retired"),
            @ApiResponse(responseCode = "404", description = "Service not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @DeleteMapping("/{uuid}")
    public ResponseEntity<BarberServiceReadOnlyDTO> deleteService(@PathVariable UUID uuid)
            throws EntityNotFoundException {
        return ResponseEntity.ok(serviceCatalogService.deleteService(uuid));
    }
}
