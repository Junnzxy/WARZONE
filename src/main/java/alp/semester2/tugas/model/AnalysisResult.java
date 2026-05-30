package alp.semester2.tugas.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AnalysisResult
 * -----------------------------------------------
 * Hasil analisa yang ditampilkan di halaman /hasil/{sessionId}.
 * Dibuat otomatis oleh sistem saat user menyelesaikan chat.
 *
 * Data terbagi menjadi 2 sumber:
 *  1. Dari Gemini AI    → ringkasan, kemungkinan diagnosis, saran tindakan
 *  2. Dari Database     → solusi edukasi, link NEJM/Mayo/PubMed, tingkat risiko
 */
@Entity
@Table(name = "analysis_results", indexes = {
    @Index(name = "idx_result_user", columnList = "user_id")
})
@Data
@NoArgsConstructor
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, unique = true)
    private Long sessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // --- Data dari Gemini AI ---

    // Rangkuman gejala yang disampaikan user selama chat
    @Column(name = "ringkasan_gejala", nullable = false, columnDefinition = "TEXT")
    private String ringkasanGejala;

    // Penjelasan kemungkinan kondisi (bukan diagnosis resmi)
    @Column(name = "kemungkinan_diagnosis", nullable = false, columnDefinition = "TEXT")
    private String kemungkinanDiagnosis;

    // Tingkat risiko yang ditentukan Gemini, dikonfirmasi dari DB
    @Enumerated(EnumType.STRING)
    @Column(name = "tingkat_risiko", nullable = false, length = 10)
    private Disease.TingkatRisiko tingkatRisiko;

    // Saran tindakan dari Gemini
    @Column(name = "saran_tindakan", nullable = false, columnDefinition = "TEXT")
    private String saranTindakan;

    // --- Penyakit yang cocok dari database kalian ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_disease_id")
    private Disease matchedDisease;

    // Disclaimer wajib
    @Column(nullable = false, columnDefinition = "TEXT")
    private String disclaimer = "Hasil ini bukan diagnosis medis resmi. " +
                                "Selalu konsultasikan ke dokter atau tenaga kesehatan profesional.";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
