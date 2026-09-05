package gr.aueb.cf.barberapp.repository;

import gr.aueb.cf.barberapp.model.Appointment;
import gr.aueb.cf.barberapp.model.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long>,
        JpaSpecificationExecutor<Appointment> {

    @EntityGraph(attributePaths = {"customer", "customer.user", "barberService", "promotion"})
    Optional<Appointment> findByUuid(UUID uuid);

    Optional<Appointment> findByUuidAndDeletedFalse(UUID uuid);

    @EntityGraph(attributePaths = {"customer", "customer.user", "barberService", "promotion"})
    Page<Appointment> findAllByDeletedFalse(Pageable pageable);

    @EntityGraph(attributePaths = {"barberService", "promotion"})
    Page<Appointment> findAllByCustomer_UuidAndDeletedFalse(UUID customerUuid, Pageable pageable);

    // Bookings that still occupy time in the diary within a window - the input to slot generation.
    @Query("""
        SELECT a FROM Appointment a
        WHERE a.deleted = false
          AND a.status IN :statuses
          AND a.startAt < :to
          AND :from < a.endAt
        ORDER BY a.startAt
        """)
    List<Appointment> findBlockingBetween(@Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to,
                                          @Param("statuses") List<AppointmentStatus> statuses);

    // The last line of defence against a double booking.
    @Query("""
        SELECT COUNT(a) > 0 FROM Appointment a
        WHERE a.deleted = false
          AND a.status IN :statuses
          AND a.startAt < :to
          AND :from < a.endAt
          AND (:excludeUuid IS NULL OR a.uuid <> :excludeUuid)
        """)
    boolean existsBlockingOverlap(@Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to,
                                  @Param("statuses") List<AppointmentStatus> statuses,
                                  @Param("excludeUuid") UUID excludeUuid);

    long countByCustomer_UuidAndStatusAndDeletedFalse(UUID customerUuid, AppointmentStatus status);
}
