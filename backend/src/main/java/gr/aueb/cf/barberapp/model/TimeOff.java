package gr.aueb.cf.barberapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

// A one-off block carved out of the weekly template: a holiday, a dentist
// appointment, an afternoon off. Subtracted from availability.
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "time_off")
public class TimeOff extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barber_id", nullable = false)
    private Barber barber;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    private String reason;

    @PrePersist
    public void initializeUUID() {
        if (uuid == null) uuid = UUID.randomUUID();
    }

    // Half-open overlap: a block ending at 10:00 does not clash with one starting at 10:00.
    public boolean overlaps(LocalDateTime otherStart, LocalDateTime otherEnd) {
        return startAt.isBefore(otherEnd) && otherStart.isBefore(endAt);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TimeOff timeOff)) return false;
        return Objects.equals(getUuid(), timeOff.getUuid());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getUuid());
    }
}
