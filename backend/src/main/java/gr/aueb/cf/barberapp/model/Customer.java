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
@Table(name = "customers")
public class Customer extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID uuid;

    @OneToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // The barber's own notes: "prefers a number 2 on the sides".
    @Column(length = 1000)
    private String notes;

    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    private List<Appointment> appointments = new ArrayList<>();

    @PrePersist
    public void initializeUUID() {
        if (uuid == null) uuid = UUID.randomUUID();
    }

    public void addUser(User user) {
        this.user = user;
        user.setCustomer(this);
    }

    public void addAppointment(Appointment appointment) {
        appointments.add(appointment);
        appointment.setCustomer(this);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Customer customer)) return false;
        return Objects.equals(getUuid(), customer.getUuid());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getUuid());
    }
}
