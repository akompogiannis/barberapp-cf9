package gr.aueb.cf.barberapp.validator;

import gr.aueb.cf.barberapp.dto.AppointmentInsertDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.time.Clock;
import java.time.LocalDateTime;

// Cheap, request-shaped checks on a booking.
@Component
@RequiredArgsConstructor
public class AppointmentInsertValidator implements Validator {

    private final Clock clock;

    @Override
    public boolean supports(Class<?> clazz) {
        return AppointmentInsertDTO.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        AppointmentInsertDTO dto = (AppointmentInsertDTO) target;

        if (dto.startAt() == null) {
            errors.rejectValue("startAt", "empty", "A start time is required");
            return;
        }

        if (dto.startAt().isBefore(LocalDateTime.now(clock))) {
            errors.rejectValue("startAt", "past", "Cannot book a time in the past");
        }

        if (dto.startAt().getSecond() != 0 || dto.startAt().getNano() != 0) {
            errors.rejectValue("startAt", "precision", "Appointment times must fall on a whole minute");
        }
    }
}
