package alp.semester2.tugas.repository;

import alp.semester2.tugas.model.AnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {
    Optional<AnalysisResult> findBySessionId(Long sessionId);
    List<AnalysisResult> findByUserIdOrderByCreatedAtDesc(Long userId);
}
