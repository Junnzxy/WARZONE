package alp.semester2.tugas.controller;

import alp.semester2.tugas.dto.ChatDto;
import alp.semester2.tugas.model.ChatSession;
import alp.semester2.tugas.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ChatController
 * -----------------------------------------------
 * POST /api/chat/start              → mulai sesi chat baru
 * POST /api/chat/{id}/message       → kirim pesan dalam sesi
 * POST /api/chat/{id}/finish        → selesaikan sesi → buat hasil analisa
 * GET  /api/chat/{id}/result        → ambil hasil analisa
 * GET  /api/chat/history            → riwayat sesi user
 *
 * Semua endpoint butuh login (lihat SecurityConfig).
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService   chatService;
    private final alp.semester2.tugas.repository.UserRepository userRepo;

    public ChatController(ChatService chatService,
                          alp.semester2.tugas.repository.UserRepository userRepo) {
        this.chatService = chatService;
        this.userRepo    = userRepo;
    }

    /**
     * Mulai sesi chat baru.
     * Response: { "sessionId": 1 }
     */
    @PostMapping("/start")
    public ResponseEntity<ChatDto.StartSessionResponse> start(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        ChatSession session = chatService.startSession(userId);
        return ResponseEntity.ok(new ChatDto.StartSessionResponse(session.getId()));
    }

    /**
     * Kirim pesan dalam sesi yang sedang berjalan.
     * Request:  { "message": "saya demam 3 hari dan kepala pusing" }
     * Response: { "reply": "...", "analysisReady": false }
     *
     * Ketika analysisReady = true, frontend menampilkan tombol "Lihat Hasil".
     */
    @PostMapping("/{sessionId}/message")
    public ResponseEntity<ChatDto.MessageResponse> sendMessage(
            @PathVariable Long sessionId,
            @Valid @RequestBody ChatDto.MessageRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getUserId(userDetails);
        ChatDto.MessageResponse response = chatService.sendMessage(
            sessionId, userId, request.getMessage()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Selesaikan sesi dan buat halaman hasil analisa.
     * Dipanggil oleh frontend saat user klik "Lihat Hasil Analisa".
     *
     * Response: AnalysisResponse lengkap (data Gemini + data DB kalian)
     */
    @PostMapping("/{sessionId}/finish")
    public ResponseEntity<ChatDto.AnalysisResponse> finish(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getUserId(userDetails);
        ChatDto.AnalysisResponse result = chatService.finishSession(sessionId, userId);
        return ResponseEntity.ok(result);
    }

    /**
     * Ambil hasil analisa yang sudah tersimpan.
     * Digunakan untuk menampilkan halaman /hasil/{sessionId}.
     */
    @GetMapping("/{sessionId}/result")
    public ResponseEntity<ChatDto.AnalysisResponse> getResult(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(chatService.getResult(sessionId, userId));
    }

    /**
     * Riwayat semua sesi chat user.
     */
    @GetMapping("/history")
    public ResponseEntity<List<ChatDto.SessionSummary>> history(
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(chatService.getSessionHistory(userId));
    }

    // -----------------------------------------------
    // Helper: ambil userId dari Spring Security context
    // -----------------------------------------------
    private Long getUserId(UserDetails userDetails) {
        return userRepo.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User tidak ditemukan"))
            .getId();
    }
}
