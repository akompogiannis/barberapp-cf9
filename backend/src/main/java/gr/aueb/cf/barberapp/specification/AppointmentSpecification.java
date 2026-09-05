package gr.aueb.cf.barberapp.specification;

import gr.aueb.cf.barberapp.core.filters.AppointmentFilters;
import gr.aueb.cf.barberapp.model.Appointment;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

// Builds the barber's diary query from whichever filters were supplied.
public class AppointmentSpecification {

    private AppointmentSpecification() {
    }

    public static Specification<Appointment> build(AppointmentFilters filters) {
        return Specification.allOf(
                hasCustomerUuid(filters.getCustomerUuid()),
                hasServiceUuid(filters.getServiceUuid()),
                hasStatus(filters.getStatus()),
                startsOnOrAfter(filters.getFrom()),
                startsBefore(filters.getTo()),
                hasCustomerLastname(filters.getCustomerLastname()),
                isDeleted(filters.isDeleted())
        );
    }

    private static Specification<Appointment> hasCustomerUuid(UUID customerUuid) {
        return (root, query, cb) -> customerUuid == null ? cb.conjunction() :
                cb.equal(root.get("customer").get("uuid"), customerUuid);
    }

    private static Specification<Appointment> hasServiceUuid(UUID serviceUuid) {
        return (root, query, cb) -> serviceUuid == null ? cb.conjunction() :
                cb.equal(root.get("barberService").get("uuid"), serviceUuid);
    }

    private static Specification<Appointment> hasStatus(
            gr.aueb.cf.barberapp.model.AppointmentStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() :
                cb.equal(root.get("status"), status);
    }

    private static Specification<Appointment> startsOnOrAfter(LocalDate from) {
        return (root, query, cb) -> from == null ? cb.conjunction() :
                cb.greaterThanOrEqualTo(root.get("startAt"), from.atStartOfDay());
    }

    // to is inclusive as a date, so the bound is the start of the next day.
    private static Specification<Appointment> startsBefore(LocalDate to) {
        return (root, query, cb) -> to == null ? cb.conjunction() :
                cb.lessThan(root.get("startAt"), to.plusDays(1).atStartOfDay());
    }

    private static Specification<Appointment> hasCustomerLastname(String lastname) {
        return (root, query, cb) -> {
            if (lastname == null || lastname.isBlank()) return cb.conjunction();
            var user = root.join("customer", JoinType.INNER).join("user", JoinType.INNER);
            return cb.like(cb.lower(user.get("lastname")), lastname.toLowerCase() + "%");
        };
    }

    private static Specification<Appointment> isDeleted(boolean deleted) {
        return (root, query, cb) -> cb.equal(root.get("deleted"), deleted);
    }
}
