package gr.aueb.cf.barberapp.mapper;

import gr.aueb.cf.barberapp.dto.*;
import gr.aueb.cf.barberapp.model.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// Entity to DTO translation, hand-written and kept in one place.
@Component
public class Mapper {

    // ---------- User ----------

    public User mapToUserEntity(UserInsertDTO dto) {
        User user = new User();
        user.setUsername(dto.username());
        user.setPassword(dto.password());        // hashed by the service, never here
        user.setFirstname(dto.firstname());
        user.setLastname(dto.lastname());
        user.setEmail(dto.email());
        user.setPhone(dto.phone());
        return user;
    }

    public UserReadOnlyDTO mapToUserReadOnlyDTO(User user) {
        Set<String> capabilities = user.getRole().getAllCapabilities().stream()
                .map(Capability::getName)
                .collect(Collectors.toSet());

        String customerUuid = user.getCustomer() != null
                ? user.getCustomer().getUuid().toString()
                : null;

        return new UserReadOnlyDTO(
                user.getUuid().toString(),
                user.getUsername(),
                user.getFirstname(),
                user.getLastname(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().getName(),
                capabilities,
                customerUuid
        );
    }

    // ---------- Services ----------

    // Catalogue entry with no promotion applied.
    public BarberServiceReadOnlyDTO mapToServiceReadOnlyDTO(BarberService service) {
        return mapToServiceReadOnlyDTO(service, null);
    }

    // Catalogue entry, optionally priced with a live promotion.
    public BarberServiceReadOnlyDTO mapToServiceReadOnlyDTO(BarberService service, Promotion promotion) {
        BigDecimal promoPrice = promotion != null ? promotion.applyTo(service.getPrice()) : null;
        String promoTitle = promotion != null ? promotion.getTitle() : null;

        return new BarberServiceReadOnlyDTO(
                service.getUuid().toString(),
                service.getName(),
                service.getDescription(),
                service.getDurationMinutes(),
                service.getPrice(),
                promoPrice,
                promoTitle,
                service.isActive()
        );
    }

    // ---------- Appointments ----------

    public AppointmentReadOnlyDTO mapToAppointmentReadOnlyDTO(Appointment appointment) {
        User customerUser = appointment.getCustomer().getUser();
        BarberService service = appointment.getBarberService();

        return new AppointmentReadOnlyDTO(
                appointment.getUuid().toString(),
                appointment.getCustomer().getUuid().toString(),
                customerUser.getFullname(),
                customerUser.getPhone(),
                service.getUuid().toString(),
                service.getName(),
                service.getDurationMinutes(),
                appointment.getStartAt(),
                appointment.getEndAt(),
                appointment.getStatus().name(),
                appointment.getPriceCharged(),
                appointment.getPromotion() != null ? appointment.getPromotion().getTitle() : null,
                appointment.getNotes()
        );
    }

    // ---------- Promotions ----------

    public PromotionReadOnlyDTO mapToPromotionReadOnlyDTO(Promotion promotion) {
        List<String> serviceNames = promotion.getServices().stream()
                .map(BarberService::getName)
                .sorted()
                .toList();

        return new PromotionReadOnlyDTO(
                promotion.getUuid().toString(),
                promotion.getTitle(),
                promotion.getDescription(),
                promotion.getDiscountPercent(),
                promotion.getValidFrom(),
                promotion.getValidTo(),
                promotion.isActive(),
                serviceNames
        );
    }

    // ---------- Schedule ----------

    public WorkingHoursDTO mapToWorkingHoursDTO(WorkingHours hours) {
        return new WorkingHoursDTO(
                hours.getId(),
                hours.getDayOfWeek(),
                hours.getStartTime(),
                hours.getEndTime()
        );
    }

    public TimeOffReadOnlyDTO mapToTimeOffReadOnlyDTO(TimeOff timeOff) {
        return new TimeOffReadOnlyDTO(
                timeOff.getUuid().toString(),
                timeOff.getStartAt(),
                timeOff.getEndAt(),
                timeOff.getReason()
        );
    }

    // ---------- Barber profile ----------

    public BarberProfileDTO mapToBarberProfileDTO(Barber barber,
                                                  List<BarberServiceReadOnlyDTO> services,
                                                  List<PromotionReadOnlyDTO> promotions,
                                                  List<WorkingHours> workingHours) {
        List<WorkingHoursDTO> hours = workingHours.stream()
                .sorted(Comparator.comparing(WorkingHours::getDayOfWeek)
                        .thenComparing(WorkingHours::getStartTime))
                .map(this::mapToWorkingHoursDTO)
                .toList();

        return new BarberProfileDTO(
                barber.getUuid().toString(),
                barber.getShopName(),
                barber.getBio(),
                barber.getAddress(),
                barber.getUser().getPhone(),
                barber.getPhotoUrl(),
                services,
                promotions,
                hours
        );
    }
}
