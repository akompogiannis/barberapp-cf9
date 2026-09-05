package gr.aueb.cf.barberapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

// The booking itself. endAt is derived from the service duration when the appointment is created
// and then persisted, rather than recomputed on read.
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "appointments")
public class Appointment extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barber_service_id", nullable = false)
    private BarberService barberService;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status = AppointmentStatus.PENDING;

    @Column(name = "price_charged", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceCharged;

    @Column(length = 1000)
    private String notes;

    @PrePersist
    public void initializeUUID() {
        if (uuid == null) uuid = UUID.randomUUID();
    }

    // Half-open overlap: an appointment ending at 10:00 leaves 10:00 free.
    public boolean overlaps(LocalDateTime otherStart, LocalDateTime otherEnd) {
        return startAt.isBefore(otherEnd) && otherStart.isBefore(endAt);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Appointment that)) return false;
        return Objects.equals(getUuid(), that.getUuid());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getUuid());
    }
}
