package alp.semester2.tugas.service;

import alp.semester2.tugas.dto.ChatDto;
import alp.semester2.tugas.model.*;
import alp.semester2.tugas.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * ChatService
 * -----------------------------------------------
 * Logika utama untuk fitur chatbot dan hasil analisa.
 *
 * Alur:
 *  startSession()   → buat chat_sessions baru
 *  sendMessage()    → simpan pesan, kirim ke Gemini, simpan balasan
 *  finishSession()  → generate AnalysisResult, cocokkan dengan diseases DB
 *  getResult()      → ambil hasil analisa + data penyakit dari DB
 */
@Service
public class ChatService {

    private final GeminiService         geminiService;
    private final ChatSessionRepository sessionRepo;
    private final ChatMessageRepository messageRepo;
    private final AnalysisResultRepository analysisRepo;
    private final DiseaseRepository     diseaseRepo;

    public ChatService(GeminiService geminiService,
                       ChatSessionRepository sessionRepo,
                       ChatMessageRepository messageRepo,
                       AnalysisResultRepository analysisRepo,
                       DiseaseRepository diseaseRepo) {
        this.geminiService = geminiService;
        this.sessionRepo   = sessionRepo;
        this.messageRepo   = messageRepo;
        this.analysisRepo  = analysisRepo;
        this.diseaseRepo   = diseaseRepo;
    }

    // -----------------------------------------------
    // 1. Mulai sesi chat baru
    // -----------------------------------------------
    @Transactional
    public ChatSession startSession(Long userId) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setStatus(ChatSession.Status.active);
        return sessionRepo.save(session);
    }

    // -----------------------------------------------
    // 2. Kirim pesan — simpan pesan user, panggil Gemini, simpan balasan
    // -----------------------------------------------
    @Transactional
    public ChatDto.MessageResponse sendMessage(Long sessionId, Long userId, String userMessage) {
        // Validasi sesi
        ChatSession session = getSessionOrThrow(sessionId, userId);
        if (session.getStatus() == ChatSession.Status.completed) {
            throw new IllegalStateException("Sesi sudah selesai.");
        }

        // Simpan pesan user ke DB
        ChatMessage userMsg = new ChatMessage(sessionId, ChatMessage.Role.user, userMessage);
        messageRepo.save(userMsg);

        // Ambil seluruh riwayat untuk dikirim ke Gemini
        List<Map<String, Object>> geminiHistory = buildGeminiHistory(sessionId);

        try {
            GeminiService.ChatResult result = geminiService.chat(geminiHistory, userMessage);

            // Simpan balasan Gemini ke DB
            ChatMessage assistantMsg = new ChatMessage(
                sessionId, ChatMessage.Role.assistant, result.reply()
            );
            messageRepo.save(assistantMsg);

            return new ChatDto.MessageResponse(result.reply(), result.analysisReady());

        } catch (Exception e) {
            throw new RuntimeException("Gagal terhubung ke AI: " + e.getMessage(), e);
        }
    }

    // -----------------------------------------------
    // 3. Selesaikan sesi → generate hasil analisa
    // -----------------------------------------------
    @Transactional
    public ChatDto.AnalysisResponse finishSession(Long sessionId, Long userId) {
        ChatSession session = getSessionOrThrow(sessionId, userId);

        // Ambil riwayat percakapan
        List<Map<String, Object>> geminiHistory = buildGeminiHistory(sessionId);

        if (geminiHistory.isEmpty()) {
            throw new IllegalStateException("Tidak ada percakapan dalam sesi ini.");
        }

        try {
            // Gemini generate analisa dari seluruh percakapan
            GeminiService.AnalysisData analysisData = geminiService.generateAnalysis(geminiHistory);

            // Cocokkan penyakit dari database kalian berdasarkan keywords
            Disease matchedDisease = findBestMatchDisease(analysisData.getKeywords());

            // Jika disease ditemukan, gunakan tingkat risiko dari DB
            // Jika tidak, gunakan hasil Gemini
            String finalRisk = matchedDisease != null
                ? matchedDisease.getTingkatRisiko().name()
                : analysisData.getTingkatRisiko();

            // Simpan hasil ke DB
            AnalysisResult resultEntity = new AnalysisResult();
            resultEntity.setSessionId(sessionId);
            resultEntity.setUserId(userId);
            resultEntity.setRingkasanGejala(analysisData.getRingkasanGejala());
            resultEntity.setKemungkinanDiagnosis(analysisData.getKemungkinanDiagnosis());
            resultEntity.setTingkatRisiko(Disease.TingkatRisiko.valueOf(finalRisk));
            resultEntity.setSaranTindakan(analysisData.getSaranTindakan());
            resultEntity.setDisclaimer(analysisData.getDisclaimer());
            resultEntity.setMatchedDisease(matchedDisease);
            analysisRepo.save(resultEntity);

            // Update status sesi menjadi completed
            session.setStatus(ChatSession.Status.completed);
            session.setCompletedAt(LocalDateTime.now());
            sessionRepo.save(session);

            return buildAnalysisResponse(resultEntity);

        } catch (Exception e) {
            throw new RuntimeException("Gagal membuat hasil analisa: " + e.getMessage(), e);
        }
    }

    // -----------------------------------------------
    // 4. Ambil hasil analisa yang sudah tersimpan
    // -----------------------------------------------
    public ChatDto.AnalysisResponse getResult(Long sessionId, Long userId) {
        AnalysisResult result = analysisRepo.findBySessionId(sessionId)
            .orElseThrow(() -> new RuntimeException("Hasil analisa tidak ditemukan."));

        if (!result.getUserId().equals(userId)) {
            throw new SecurityException("Akses ditolak.");
        }

        return buildAnalysisResponse(result);
    }

    // -----------------------------------------------
    // 5. Riwayat sesi user
    // -----------------------------------------------
    public List<ChatDto.SessionSummary> getSessionHistory(Long userId) {
        return sessionRepo.findByUserIdOrderByStartedAtDesc(userId)
            .stream()
            .map(s -> {
                ChatDto.SessionSummary summary = new ChatDto.SessionSummary();
                summary.setSessionId(s.getId());
                summary.setStatus(s.getStatus().name());
                summary.setStartedAt(s.getStartedAt());
                summary.setCompletedAt(s.getCompletedAt());

                // Ambil tingkat risiko jika sudah selesai
                if (s.getStatus() == ChatSession.Status.completed) {
                    analysisRepo.findBySessionId(s.getId()).ifPresent(r ->
                        summary.setTingkatRisiko(r.getTingkatRisiko().name())
                    );
                }

                return summary;
            })
            .toList();
    }

    // -----------------------------------------------
    // Private helpers
    // -----------------------------------------------

    private ChatSession getSessionOrThrow(Long sessionId, Long userId) {
        ChatSession session = sessionRepo.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Sesi tidak ditemukan."));
        if (!session.getUserId().equals(userId)) {
            throw new SecurityException("Akses ditolak.");
        }
        return session;
    }

    /**
     * Ubah riwayat chat dari DB ke format yang dimengerti Gemini API.
     * Format: [{"role": "user", "parts": [{"text": "..."}]}, ...]
     */
    private List<Map<String, Object>> buildGeminiHistory(Long sessionId) {
        return messageRepo.findBySessionIdOrderByCreatedAtAsc(sessionId)
            .stream()
            .map(msg -> {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("role",  msg.getRole().name());
                entry.put("parts", List.of(Map.of("text", msg.getContent())));
                return entry;
            })
            .toList();
    }

    /**
     * Cari penyakit terbaik dari database berdasarkan keywords Gemini.
     * Iterasi setiap keyword sampai ada yang cocok.
     */
    private Disease findBestMatchDisease(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return null;

        for (String keyword : keywords) {
            List<Disease> found = diseaseRepo.findByKeyword(keyword.trim());
            if (!found.isEmpty()) return found.get(0);
        }
        return null;
    }

    /**
     * Bangun AnalysisResponse dari AnalysisResult entity.
     * Menggabungkan data Gemini + data database kalian.
     */
    private ChatDto.AnalysisResponse buildAnalysisResponse(AnalysisResult result) {
        ChatDto.AnalysisResponse response = new ChatDto.AnalysisResponse();
        response.setSessionId(result.getSessionId());
        response.setRingkasanGejala(result.getRingkasanGejala());
        response.setKemungkinanDiagnosis(result.getKemungkinanDiagnosis());
        response.setTingkatRisiko(result.getTingkatRisiko().name());
        response.setSaranTindakan(result.getSaranTindakan());
        response.setDisclaimer(result.getDisclaimer());
        response.setCreatedAt(result.getCreatedAt());

        // Data penyakit dari database kalian (solusi + link referensi)
        if (result.getMatchedDisease() != null) {
            response.setPenyakit(ChatDto.AnalysisResponse.DiseaseInfo.from(result.getMatchedDisease()));
        }

        return response;
    }
}
