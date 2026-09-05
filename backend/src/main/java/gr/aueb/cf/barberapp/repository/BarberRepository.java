package gr.aueb.cf.barberapp.repository;

import gr.aueb.cf.barberapp.model.Barber;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BarberRepository extends JpaRepository<Barber, Long> {

    Optional<Barber> findByUuid(UUID uuid);
    Optional<Barber> findByUuidAndDeletedFalse(UUID uuid);

    @EntityGraph(attributePaths = {"user"})
    Optional<Barber> findFirstByDeletedFalseOrderByIdAsc();

    Optional<Barber> findByUser_Username(String username);
}
