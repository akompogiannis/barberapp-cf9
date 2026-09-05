package gr.aueb.cf.barberapp.repository;

import gr.aueb.cf.barberapp.model.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    @EntityGraph(attributePaths = {"services"})
    Optional<Promotion> findByUuid(UUID uuid);

    Optional<Promotion> findByUuidAndDeletedFalse(UUID uuid);

    @EntityGraph(attributePaths = {"services"})
    Page<Promotion> findAllByDeletedFalse(Pageable pageable);

    // What the public landing page advertises today.
    @EntityGraph(attributePaths = {"services"})
    @Query("""
        SELECT p FROM Promotion p
        WHERE p.deleted = false
          AND p.active = true
          AND p.validFrom <= :date
          AND p.validTo >= :date
        ORDER BY p.discountPercent DESC
        """)
    List<Promotion> findActiveOn(@Param("date") LocalDate date);

    // The best live promotion for one service on one day, if any.
    // Ordered by discount so the customer is quoted the cheapest price.
    @Query("""
        SELECT p FROM Promotion p
        JOIN p.services s
        WHERE p.deleted = false
          AND p.active = true
          AND p.validFrom <= :date
          AND p.validTo >= :date
          AND s.id = :serviceId
        ORDER BY p.discountPercent DESC
        """)
    List<Promotion> findApplicable(@Param("serviceId") Long serviceId, @Param("date") LocalDate date);
}
