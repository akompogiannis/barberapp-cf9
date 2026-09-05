package gr.aueb.cf.barberapp.repository;

import gr.aueb.cf.barberapp.model.WorkingHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;

@Repository
public interface WorkingHoursRepository extends JpaRepository<WorkingHours, Long> {

    List<WorkingHours> findAllByBarber_IdAndDeletedFalseOrderByDayOfWeekAscStartTimeAsc(Long barberId);

    // The blocks that make up a single day, in order - the basis of slot generation.
    List<WorkingHours> findAllByBarber_IdAndDayOfWeekAndDeletedFalseOrderByStartTimeAsc(
            Long barberId, DayOfWeek dayOfWeek);
}
