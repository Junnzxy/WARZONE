package alp.semester2.tugas.dto;

import alp.semester2.tugas.model.Disease;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public class ChatDto {

    // ----- Kirim pesan -----
    @Data
    public static class MessageRequest {
        @NotBlank(message = "Pesan tidak boleh kosong")
        @Size(min = 2, max = 1000)
        private String message;
    }

    // ----- Balasan dari AI -----
    @Data
    public static class MessageResponse {
        private String  reply;
        private boolean analysisReady; // true = Gemini sudah cukup info → bisa finish

        public MessageResponse(String reply, boolean analysisReady) {
            this.reply         = reply;
            this.analysisReady = analysisReady;
        }
    }

    // ----- Mulai sesi -----
    @Data
    public static class StartSessionResponse {
        private Long sessionId;
        public StartSessionResponse(Long sessionId) { this.sessionId = sessionId; }
    }

    // ----- Hasil analisa lengkap (halaman /hasil) -----
    @Data
    public static class AnalysisResponse {

        // Dari Gemini AI
        private String ringkasanGejala;
        private String kemungkinanDiagnosis;
        private String tingkatRisiko;
        private String saranTindakan;
        private String disclaimer;

        // Dari Database kalian
        private DiseaseInfo penyakit;

        // Metadata
        private Long          sessionId;
        private LocalDateTime createdAt;

        @Data
        public static class DiseaseInfo {
            private Long   id;
            private String namaPenyakit;
            private String medicalTerm;
            private String kategori;
            // Solusi edukasi dari database kalian — BUKAN dari AI
            private String solusiEdukasi;
            private String tingkatRisiko;
            // Link referensi dari database kalian
            private String urlNejm;
            private String urlMayoClinic;
            private String urlPubmed;

            public static DiseaseInfo from(Disease d) {
                DiseaseInfo info = new DiseaseInfo();
                info.setId(d.getId());
                info.setNamaPenyakit(d.getNamaPenyakit());
                info.setMedicalTerm(d.getMedicalTerm());
                info.setKategori(d.getKategori());
                info.setSolusiEdukasi(d.getSolusiEdukasi());
                info.setTingkatRisiko(d.getTingkatRisiko().name());
                info.setUrlNejm(d.getUrlNejm());
                info.setUrlMayoClinic(d.getUrlMayoClinic());
                info.setUrlPubmed(d.getUrlPubmed());
                return info;
            }
        }
    }

    // ----- Riwayat sesi -----
    @Data
    public static class SessionSummary {
        private Long          sessionId;
        private String        status;
        private String        tingkatRisiko; // dari hasil analisa (jika sudah selesai)
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
    }
}
