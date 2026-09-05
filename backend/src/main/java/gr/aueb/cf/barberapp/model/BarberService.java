package gr.aueb.cf.barberapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "barber_services")
public class BarberService extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barber_id", nullable = false)
    private Barber barber;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    // Retired services stay bookable in history but are no longer offered.
    @Column(nullable = false)
    private boolean active = true;

    @ManyToMany(mappedBy = "services", fetch = FetchType.LAZY)
    private Set<Promotion> promotions = new HashSet<>();

    @PrePersist
    public void initializeUUID() {
        if (uuid == null) uuid = UUID.randomUUID();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BarberService that)) return false;
        return Objects.equals(getUuid(), that.getUuid());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getUuid());
    }
}
