package gr.aueb.cf.barberapp.repository;

import gr.aueb.cf.barberapp.model.BarberService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BarberServiceRepository extends JpaRepository<BarberService, Long> {

    Optional<BarberService> findByUuid(UUID uuid);
    Optional<BarberService> findByUuidAndDeletedFalse(UUID uuid);

    // What the public catalogue shows.
    List<BarberService> findAllByDeletedFalseAndActiveTrueOrderByNameAsc();

    Page<BarberService> findAllByDeletedFalse(Pageable pageable);

    boolean existsByBarber_IdAndNameIgnoreCaseAndDeletedFalse(Long barberId, String name);
}
