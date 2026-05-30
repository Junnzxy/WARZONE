package alp.semester2.tugas.repository;

import alp.semester2.tugas.model.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    List<ChatSession> findByUserIdOrderByStartedAtDesc(Long userId);
    List<ChatSession> findByUserIdAndStatus(Long userId, ChatSession.Status status);
}
