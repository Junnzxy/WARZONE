package alp.semester2.tugas.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "reminders", indexes = {
    @Index(name = "idx_reminder_user", columnList = "user_id")
})
@Data
@NoArgsConstructor
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Type type;

    // Deskripsi pengingat, mis. "Minum obat hipertensi"
    @Column(nullable = false, length = 200)
    private String label;

    @Column(name = "schedule_time", nullable = false)
    private LocalTime scheduleTime;

    // "daily" atau "Mon,Wed,Fri"
    @Column(name = "repeat_days", nullable = false, length = 50)
    private String repeatDays;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum Type {
        periksa, obat, pola_hidup
    }
}
