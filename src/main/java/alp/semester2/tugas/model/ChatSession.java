package alp.semester2.tugas.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ChatSession
 * -----------------------------------------------
 * Satu sesi percakapan chatbot.
 * Status "active"    → user sedang chat.
 * Status "completed" → chat selesai, hasil analisa sudah dibuat.
 */
@Entity
@Table(name = "chat_sessions", indexes = {
    @Index(name = "idx_session_user",  columnList = "user_id"),
    @Index(name = "idx_session_status",columnList = "status")
})
@Data
@NoArgsConstructor
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Status status = Status.active;

    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public enum Status {
        active, completed
    }
}
