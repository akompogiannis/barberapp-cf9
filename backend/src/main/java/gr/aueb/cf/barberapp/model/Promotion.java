package gr.aueb.cf.barberapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

// A seasonal discount the barber advertises. Applies to a chosen subset of
// services and is only honoured inside its validity window.
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "promotions")
public class Promotion extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barber_id", nullable = false)
    private Barber barber;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(name = "discount_percent", nullable = false)
    private Integer discountPercent;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDate validTo;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "promotions_services",
            joinColumns = @JoinColumn(name = "promotion_id"),
            inverseJoinColumns = @JoinColumn(name = "barber_service_id")
    )
    private Set<BarberService> services = new HashSet<>();

    @PrePersist
    public void initializeUUID() {
        if (uuid == null) uuid = UUID.randomUUID();
    }

    public void addService(BarberService service) {
        services.add(service);
        service.getPromotions().add(this);
    }

    public void removeService(BarberService service) {
        services.remove(service);
        service.getPromotions().remove(this);
    }

    // Live on the given day: switched on, not deleted, and inside the window.
    public boolean isValidOn(LocalDate date) {
        return active
                && !isDeleted()
                && !date.isBefore(validFrom)
                && !date.isAfter(validTo);
    }

    public boolean appliesTo(BarberService service) {
        return services.contains(service);
    }

    // The price actually charged once this promotion is applied.
    // Rounded half-up to the cent, which is how money is quoted to the customer.
    public BigDecimal applyTo(BigDecimal price) {
        BigDecimal multiplier = BigDecimal.valueOf(100 - discountPercent)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return price.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Promotion promotion)) return false;
        return Objects.equals(getUuid(), promotion.getUuid());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getUuid());
    }
}
