package gr.aueb.cf.barberapp.api;

import gr.aueb.cf.barberapp.core.exceptions.*;
import gr.aueb.cf.barberapp.core.filters.AppointmentFilters;
import gr.aueb.cf.barberapp.dto.*;
import gr.aueb.cf.barberapp.service.IAppointmentService;
import gr.aueb.cf.barberapp.validator.AppointmentInsertValidator;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Booking and diary management")
public class AppointmentRestController {

    private final IAppointmentService appointmentService;
    private final AppointmentInsertValidator appointmentInsertValidator;

    @Operation(
            summary = "Book an appointment",
            description = """
                    Requires BOOK_APPOINTMENT. The booking is made for the account in the token,
                    so a customer cannot book on somebody else's behalf.

                    The end time and the price are both computed server-side from the service and
                    any live promotion. A 409 means the slot was taken between loading the
                    calendar and pressing book - refresh the availability and pick another.
                    """
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Appointment booked",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AppointmentReadOnlyDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ValidationErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Service not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "The slot is not available",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping
    public ResponseEntity<AppointmentReadOnlyDTO> book(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody AppointmentInsertDTO dto,
            BindingResult bindingResult)
            throws EntityNotFoundException, SlotUnavailableException,
                   EntityInvalidArgumentException, ValidationException {

        appointmentInsertValidator.validate(dto, bindingResult);
        if (bindingResult.hasErrors()) {
            throw new ValidationException("Appointment", "Invalid booking data", bindingResult);
        }

        AppointmentReadOnlyDTO created = appointmentService.book(principal.getUsername(), dto);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{uuid}")
                .buildAndExpand(created.uuid())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @Operation(
            summary = "Browse the diary",
            description = """
                    Requires VIEW_APPOINTMENTS, so this is the barber's view of every booking.
                    All filter parameters are optional and combine.
                    """
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponse(responseCode = "200", description = "Appointments returned")
    @GetMapping
    public ResponseEntity<Page<AppointmentReadOnlyDTO>> getAppointments(
            @PageableDefault(page = 0, size = 10, sort = "startAt") Pageable pageable,
            @ModelAttribute AppointmentFilters filters) {

        return ResponseEntity.ok(appointmentService.getAppointments(pageable, filters));
    }

    @Operation(
            summary = "List the signed-in customer's own appointments",
            description = "Requires VIEW_OWN_APPOINTMENTS. Scoped to the account in the token."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponse(responseCode = "200", description = "Appointments returned")
    @GetMapping("/me")
    public ResponseEntity<Page<AppointmentReadOnlyDTO>> getOwnAppointments(
            @AuthenticationPrincipal UserDetails principal,
            @PageableDefault(page = 0, size = 10, sort = "startAt") Pageable pageable)
            throws EntityNotFoundException {

        return ResponseEntity.ok(appointmentService.getOwnAppointments(principal.getUsername(), pageable));
    }

    @Operation(
            summary = "Get one appointment",
            description = "The barber can read any appointment; a customer only their own."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Appointment returned",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AppointmentReadOnlyDTO.class))),
            @ApiResponse(responseCode = "404", description = "Appointment not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{uuid}")
    public ResponseEntity<AppointmentReadOnlyDTO> getAppointment(@PathVariable UUID uuid)
            throws EntityNotFoundException {
        return ResponseEntity.ok(appointmentService.getByUuid(uuid));
    }

    @Operation(
            summary = "Move an appointment through its lifecycle",
            description = """
                    Requires EDIT_APPOINTMENT. Legal moves are PENDING to CONFIRMED or CANCELLED,
                    and CONFIRMED to COMPLETED, CANCELLED or NO_SHOW. Anything else is a 400.
                    """
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status changed",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AppointmentReadOnlyDTO.class))),
            @ApiResponse(responseCode = "400", description = "Illegal status transition",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Appointment not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PatchMapping("/{uuid}/status")
    public ResponseEntity<AppointmentReadOnlyDTO> changeStatus(
            @PathVariable UUID uuid,
            @Valid @RequestBody AppointmentUpdateStatusDTO dto)
            throws EntityNotFoundException, EntityInvalidArgumentException {

        return ResponseEntity.ok(appointmentService.changeStatus(uuid, dto.status()));
    }

    @Operation(
            summary = "Reschedule an appointment",
            description = """
                    The barber can move any appointment; a customer only their own. The new time
                    goes through the same availability checks as a fresh booking, with this
                    appointment excluded from the overlap test so it does not collide with itself.
                    """
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Appointment moved",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AppointmentReadOnlyDTO.class))),
            @ApiResponse(responseCode = "409", description = "The new slot is not available",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PatchMapping("/{uuid}/reschedule")
    public ResponseEntity<AppointmentReadOnlyDTO> reschedule(
            @PathVariable UUID uuid,
            @Valid @RequestBody AppointmentRescheduleDTO dto)
            throws EntityNotFoundException, SlotUnavailableException, EntityInvalidArgumentException {

        return ResponseEntity.ok(appointmentService.reschedule(uuid, dto.startAt()));
    }

    @Operation(
            summary = "Cancel an appointment",
            description = """
                    The barber can cancel any appointment; a customer only their own. The booking
                    is marked CANCELLED rather than deleted, which frees the slot while leaving
                    the cancellation visible in the diary.
                    """
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Appointment cancelled",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AppointmentReadOnlyDTO.class))),
            @ApiResponse(responseCode = "400", description = "Already in a terminal state",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Appointment not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @DeleteMapping("/{uuid}")
    public ResponseEntity<AppointmentReadOnlyDTO> cancel(@PathVariable UUID uuid)
            throws EntityNotFoundException, EntityInvalidArgumentException {
        return ResponseEntity.ok(appointmentService.cancel(uuid));
    }
}
