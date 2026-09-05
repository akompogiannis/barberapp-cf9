package gr.aueb.cf.barberapp.security;

import gr.aueb.cf.barberapp.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// Ownership checks referenced from @PreAuthorize expressions as
// @securityService.isOwnAppointment(#uuid, authentication).
@Service("securityService")
@RequiredArgsConstructor
public class SecurityService {

    private final AppointmentRepository appointmentRepository;

    @Transactional(readOnly = true)
    public boolean isOwnAppointment(UUID appointmentUuid, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return false;

        return appointmentRepository.findByUuid(appointmentUuid)
                .map(appointment -> appointment.getCustomer()
                        .getUser()
                        .getUsername()
                        .equals(authentication.getName()))
                .orElse(false);
    }
}
