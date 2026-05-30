package alp.semester2.tugas.repository;

import alp.semester2.tugas.model.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {
    List<Reminder> findByUserIdOrderByScheduleTimeAsc(Long userId);
    List<Reminder> findByUserIdAndIsActiveTrue(Long userId);
}
