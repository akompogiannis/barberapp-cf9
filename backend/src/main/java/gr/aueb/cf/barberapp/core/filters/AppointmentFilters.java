package gr.aueb.cf.barberapp.core.filters;

import gr.aueb.cf.barberapp.model.AppointmentStatus;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

// Optional query filters for the barber's diary.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class AppointmentFilters {

    private UUID customerUuid;
    private UUID serviceUuid;
    private AppointmentStatus status;
    private LocalDate from;
    private LocalDate to;
    private String customerLastname;

    // Deleted rows are excluded unless this is explicitly set.
    private boolean deleted;
}
