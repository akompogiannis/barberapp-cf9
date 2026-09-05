package gr.aueb.cf.barberapp.repository;

import gr.aueb.cf.barberapp.model.TimeOff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TimeOffRepository extends JpaRepository<TimeOff, Long> {

    Optional<TimeOff> findByUuid(UUID uuid);
    Optional<TimeOff> findByUuidAndDeletedFalse(UUID uuid);

    // Every block that intersects the window. Half-open on both sides, so a block
    // ending exactly at from is not returned.
    @Query("""
        SELECT t FROM TimeOff t
        WHERE t.barber.id = :barberId
          AND t.deleted = false
          AND t.startAt < :to
          AND :from < t.endAt
        ORDER BY t.startAt
        """)
    List<TimeOff> findOverlapping(@Param("barberId") Long barberId,
                                  @Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to);
}
