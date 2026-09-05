package gr.aueb.cf.barberapp.api;

import gr.aueb.cf.barberapp.core.exceptions.EntityInvalidArgumentException;
import gr.aueb.cf.barberapp.core.exceptions.EntityNotFoundException;
import gr.aueb.cf.barberapp.dto.AvailabilityDTO;
import gr.aueb.cf.barberapp.dto.ErrorResponseDTO;
import gr.aueb.cf.barberapp.service.IAvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/availability")
@RequiredArgsConstructor
@Tag(name = "Availability", description = "Free appointment slots")
public class AvailabilityRestController {

    private final IAvailabilityService availabilityService;

    @Operation(
            summary = "Get the free slots for a service on a date",
            description = """
                    Public, so the booking calendar works before anyone signs in.

                    Slots are derived on the fly from the weekly working hours, minus existing
                    bookings, minus time off, minus anything already in the past. A slot is only
                    offered if the whole service fits inside a single working block, so a
                    50-minute cut is never offered at 13:30 when the shop closes for lunch at 14:00.

                    `closed: true` with an empty list means the shop does not work that weekday,
                    which is a different message to show than "fully booked".
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Availability returned",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AvailabilityDTO.class))),
            @ApiResponse(responseCode = "400", description = "Date is in the past or too far ahead",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Service not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping
    public ResponseEntity<AvailabilityDTO> getAvailability(
            @Parameter(description = "Service to book", required = true)
            @RequestParam UUID serviceUuid,

            @Parameter(description = "Day to check, as yyyy-MM-dd", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date)
            throws EntityNotFoundException, EntityInvalidArgumentException {

        return ResponseEntity.ok(availabilityService.getAvailability(serviceUuid, date));
    }
}
