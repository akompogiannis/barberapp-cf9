package gr.aueb.cf.barberapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "barbers")
public class Barber extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID uuid;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "shop_name", nullable = false)
    private String shopName;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String address;

    @Column(name = "photo_url", length = 512)
    private String photoUrl;

    // The granularity offered slots start on. With a 15 minute step a 30 minute
    // haircut can begin at 09:00, 09:15, 09:30 ... rather than only on the hour.
    @Column(name = "slot_step_minutes", nullable = false)
    private Integer slotStepMinutes = 15;

    @OneToMany(mappedBy = "barber", fetch = FetchType.LAZY)
    private List<BarberService> services = new ArrayList<>();

    @OneToMany(mappedBy = "barber", fetch = FetchType.LAZY)
    private List<WorkingHours> workingHours = new ArrayList<>();

    @OneToMany(mappedBy = "barber", fetch = FetchType.LAZY)
    private List<TimeOff> timeOffs = new ArrayList<>();

    @OneToMany(mappedBy = "barber", fetch = FetchType.LAZY)
    private List<Promotion> promotions = new ArrayList<>();

    @PrePersist
    public void initializeUUID() {
        if (uuid == null) uuid = UUID.randomUUID();
    }

    public void addService(BarberService service) {
        services.add(service);
        service.setBarber(this);
    }

    public void addWorkingHours(WorkingHours hours) {
        workingHours.add(hours);
        hours.setBarber(this);
    }

    public void addTimeOff(TimeOff timeOff) {
        timeOffs.add(timeOff);
        timeOff.setBarber(this);
    }

    public void addPromotion(Promotion promotion) {
        promotions.add(promotion);
        promotion.setBarber(this);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Barber barber)) return false;
        return Objects.equals(getUuid(), barber.getUuid());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getUuid());
    }
}
