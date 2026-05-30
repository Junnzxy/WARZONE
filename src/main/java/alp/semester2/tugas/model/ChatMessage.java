package alp.semester2.tugas.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ChatMessage
 * -----------------------------------------------
 * Satu pesan dalam sesi chat.
 * role "user"      → pesan dari pengguna.
 * role "assistant" → balasan dari Gemini AI.
 */
@Entity
@Table(name = "chat_messages", indexes = {
    @Index(name = "idx_msg_session", columnList = "session_id")
})
@Data
@NoArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Role role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ChatMessage(Long sessionId, Role role, String content) {
        this.sessionId = sessionId;
        this.role      = role;
        this.content   = content;
    }

    public enum Role {
        user, assistant
    }
}
