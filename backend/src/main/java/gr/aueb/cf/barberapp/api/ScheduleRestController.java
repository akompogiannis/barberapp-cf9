package gr.aueb.cf.barberapp.api;

import gr.aueb.cf.barberapp.core.exceptions.EntityInvalidArgumentException;
import gr.aueb.cf.barberapp.core.exceptions.EntityNotFoundException;
import gr.aueb.cf.barberapp.core.exceptions.ValidationException;
import gr.aueb.cf.barberapp.dto.ErrorResponseDTO;
import gr.aueb.cf.barberapp.dto.TimeOffInsertDTO;
import gr.aueb.cf.barberapp.dto.TimeOffReadOnlyDTO;
import gr.aueb.cf.barberapp.dto.WorkingHoursDTO;
import gr.aueb.cf.barberapp.service.IScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/schedule")
@RequiredArgsConstructor
@Tag(name = "Schedule", description = "Weekly working hours and time off")
public class ScheduleRestController {

    private final IScheduleService scheduleService;

    @Operation(summary = "Get the weekly working hours", description = "Requires MANAGE_SCHEDULE.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponse(responseCode = "200", description = "Working hours returned")
    @GetMapping("/working-hours")
    public ResponseEntity<List<WorkingHoursDTO>> getWorkingHours() {
        return ResponseEntity.ok(scheduleService.getWorkingHours());
    }

    @Operation(
            summary = "Replace the weekly working hours",
            description = """
                    Requires MANAGE_SCHEDULE. The list replaces the whole template in one call.

                    A day with a lunch break is expressed as two blocks; the gap between them is
                    the break. Blocks on the same day may not overlap.

                    Existing appointments are not touched, since each one stored its own start
                    and end time when it was booked.
                    """
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Working hours replaced"),
            @ApiResponse(responseCode = "400", description = "Blocks overlap or are malformed",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PutMapping("/working-hours")
    public ResponseEntity<List<WorkingHoursDTO>> replaceWorkingHours(
            @Valid @RequestBody List<WorkingHoursDTO> blocks)
            throws EntityInvalidArgumentException {

        return ResponseEntity.ok(scheduleService.replaceWorkingHours(blocks));
    }

    @Operation(
            summary = "List time off",
            description = "Requires MANAGE_SCHEDULE. Both bounds are optional."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponse(responseCode = "200", description = "Time off returned")
    @GetMapping("/time-off")
    public ResponseEntity<List<TimeOffReadOnlyDTO>> getTimeOff(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        return ResponseEntity.ok(scheduleService.getTimeOff(from, to));
    }

    @Operation(
            summary = "Block out a period",
            description = """
                    Requires MANAGE_SCHEDULE. The period stops being offered as availability.

                    Appointments already booked inside it are deliberately left alone: standing a
                    customer up is the barber's decision, not a side effect of blocking an afternoon.
                    """
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Time off registered",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TimeOffReadOnlyDTO.class))),
            @ApiResponse(responseCode = "400", description = "The end does not come after the start",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/time-off")
    public ResponseEntity<TimeOffReadOnlyDTO> addTimeOff(@Valid @RequestBody TimeOffInsertDTO dto,
                                                         BindingResult bindingResult)
            throws EntityInvalidArgumentException, ValidationException {

        if (bindingResult.hasErrors()) {
            throw new ValidationException("TimeOff", "Invalid time off data", bindingResult);
        }

        TimeOffReadOnlyDTO created = scheduleService.addTimeOff(dto);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Remove a block of time off", description = "Requires MANAGE_SCHEDULE.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Time off removed"),
            @ApiResponse(responseCode = "404", description = "Time off not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @DeleteMapping("/time-off/{uuid}")
    public ResponseEntity<TimeOffReadOnlyDTO> deleteTimeOff(@PathVariable UUID uuid)
            throws EntityNotFoundException {
        return ResponseEntity.ok(scheduleService.deleteTimeOff(uuid));
    }
}
