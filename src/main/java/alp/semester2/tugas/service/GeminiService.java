package alp.semester2.tugas.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.*;
import java.util.*;

/**
 * GeminiService
 * -----------------------------------------------
 * Mengelola semua komunikasi dengan Gemini API.
 *
 * MODE 1 — chat()
 *   Gemini menggali gejala lewat percakapan.
 *   Saat info cukup → Gemini sisipkan flag ANALYSIS_READY.
 *
 * MODE 2 — generateAnalysis()
 *   Gemini merangkum seluruh percakapan → JSON terstruktur.
 *   Hasil ini dipakai untuk halaman /hasil.
 *   Solusi edukasi tetap diambil dari DB, bukan dari Gemini.
 */
@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private static final String CHAT_SYSTEM_PROMPT = """
        Kamu adalah asisten kesehatan digital bernama "SehatKu AI".
        Tugasmu adalah membantu pengguna memahami gejala yang mereka rasakan
        melalui percakapan yang ramah, empati, dan mudah dipahami.

        CARA KERJA:
        1. Sapa pengguna dengan hangat dan tanya gejala utama mereka.
        2. Gali informasi secara bertahap — jangan lebih dari 2 pertanyaan per pesan.
        3. Informasi yang perlu digali: jenis gejala, lokasi, durasi, intensitas (1-10),
           gejala tambahan, riwayat penyakit, usia pengguna.
        4. Setelah minimal 3-4 pertukaran pesan dan kamu merasa informasi cukup,
           akhiri pesanmu dengan baris berikut (HARUS di baris paling terakhir):
           ANALYSIS_READY:{"ready":true,"keywords":["nama_penyakit_1","nama_penyakit_2"],"risk":"ringan|sedang|berat"}
           keywords harus nama penyakit dalam Bahasa Indonesia.

        ATURAN PENTING:
        - Gunakan Bahasa Indonesia yang santai dan empati.
        - Jangan buat diagnosis medis.
        - Jangan rekomendasikan nama obat spesifik.
        - Jika gejala terdengar DARURAT (nyeri dada hebat, sesak napas berat,
          tidak sadarkan diri), LANGSUNG sarankan ke UGD dan set risk "berat".
        - Selalu ingatkan bahwa ini bukan pengganti dokter.
        """;

    private static final String ANALYSIS_SYSTEM_PROMPT = """
        Kamu adalah sistem analisa kesehatan digital.
        Berdasarkan riwayat percakapan yang diberikan, buat ringkasan analisa.
        
        Kembalikan HANYA JSON berikut, tanpa teks tambahan, tanpa markdown backtick:
        {
          "ringkasan_gejala": "Rangkuman gejala dalam 2-3 kalimat.",
          "kemungkinan_diagnosis": "Penjelasan kemungkinan kondisi medis dalam 2-3 kalimat. Bukan diagnosis resmi.",
          "tingkat_risiko": "ringan|sedang|berat",
          "saran_tindakan": "Langkah konkret yang sebaiknya dilakukan pengguna.",
          "keywords": ["nama_penyakit_Indonesia_1", "nama_penyakit_Indonesia_2"],
          "disclaimer": "Hasil ini bukan diagnosis medis resmi. Selalu konsultasikan ke dokter atau tenaga kesehatan profesional."
        }
        """;

    private final HttpClient   httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper     = new ObjectMapper();

    // -----------------------------------------------
    // MODE 1: Chat — kirim satu pesan, terima balasan
    // -----------------------------------------------
    public ChatResult chat(List<Map<String, Object>> history, String newMessage) throws Exception {
        // Tambah pesan baru ke history
        List<Map<String, Object>> contents = new ArrayList<>(history);
        contents.add(buildUserContent(newMessage));

        String raw = callGemini(CHAT_SYSTEM_PROMPT, contents, 0.7, 600);

        // Cek apakah Gemini menyatakan analisa siap
        boolean ready = raw.contains("ANALYSIS_READY:");
        String  reply = raw;
        String  risk  = "sedang";
        List<String> keywords = new ArrayList<>();

        if (ready) {
            int idx = raw.lastIndexOf("ANALYSIS_READY:");
            reply = raw.substring(0, idx).trim();
            String jsonPart = raw.substring(idx + "ANALYSIS_READY:".length()).trim();
            try {
                JsonNode flag = mapper.readTree(jsonPart);
                risk = flag.path("risk").asText("sedang");
                flag.path("keywords").forEach(k -> keywords.add(k.asText()));
            } catch (Exception ignored) { /* parsing aman */ }
        }

        return new ChatResult(reply, ready, keywords, risk);
    }

    // -----------------------------------------------
    // MODE 2: Generate analisa dari seluruh percakapan
    // -----------------------------------------------
    public AnalysisData generateAnalysis(List<Map<String, Object>> history) throws Exception {
        // Gabungkan semua pesan menjadi satu teks
        StringBuilder sb = new StringBuilder("Riwayat percakapan:\n\n");
        for (Map<String, Object> msg : history) {
            String role = (String) msg.get("role");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> parts = (List<Map<String, Object>>) msg.get("parts");
            String text = (String) parts.get(0).get("text");
            sb.append(role.equals("user") ? "Pengguna: " : "Asisten: ")
              .append(text).append("\n");
        }

        List<Map<String, Object>> contents = List.of(buildUserContent(sb.toString()));
        String raw = callGemini(ANALYSIS_SYSTEM_PROMPT, contents, 0.2, 1024);

        // Bersihkan markdown fence jika ada
        String clean = raw.replaceAll("(?s)```json\\s*", "")
                          .replaceAll("```", "")
                          .trim();

        JsonNode result = mapper.readTree(clean);

        AnalysisData data = new AnalysisData();
        data.setRingkasanGejala(result.path("ringkasan_gejala").asText());
        data.setKemungkinanDiagnosis(result.path("kemungkinan_diagnosis").asText());
        data.setTingkatRisiko(result.path("tingkat_risiko").asText("sedang"));
        data.setSaranTindakan(result.path("saran_tindakan").asText());
        data.setDisclaimer(result.path("disclaimer").asText());

        List<String> kws = new ArrayList<>();
        result.path("keywords").forEach(k -> kws.add(k.asText()));
        data.setKeywords(kws);

        return data;
    }

    // -----------------------------------------------
    // Helper: panggil Gemini API
    // -----------------------------------------------
    private String callGemini(String systemPrompt,
                               List<Map<String, Object>> contents,
                               double temperature,
                               int maxTokens) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("system_instruction", Map.of("parts", List.of(Map.of("text", systemPrompt))));
        body.put("contents", contents);
        body.put("generationConfig", Map.of(
            "temperature",     temperature,
            "maxOutputTokens", maxTokens
        ));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(apiUrl + "?key=" + apiKey))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
            .build();

        HttpResponse<String> response = httpClient.send(
            request, HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new RuntimeException("Gemini API error: HTTP " + response.statusCode()
                + " → " + response.body());
        }

        JsonNode root = mapper.readTree(response.body());
        return root.path("candidates").get(0)
                   .path("content").path("parts").get(0)
                   .path("text").asText();
    }

    private Map<String, Object> buildUserContent(String text) {
        return Map.of("role", "user", "parts", List.of(Map.of("text", text)));
    }

    // -----------------------------------------------
    // Inner result classes
    // -----------------------------------------------
    public record ChatResult(
        String reply,
        boolean analysisReady,
        List<String> keywords,
        String riskLevel
    ) {}

    public static class AnalysisData {
        private String ringkasanGejala;
        private String kemungkinanDiagnosis;
        private String tingkatRisiko;
        private String saranTindakan;
        private String disclaimer;
        private List<String> keywords;

        public String getRingkasanGejala()          { return ringkasanGejala; }
        public void   setRingkasanGejala(String v)  { ringkasanGejala = v; }
        public String getKemungkinanDiagnosis()     { return kemungkinanDiagnosis; }
        public void   setKemungkinanDiagnosis(String v){ kemungkinanDiagnosis = v; }
        public String getTingkatRisiko()            { return tingkatRisiko; }
        public void   setTingkatRisiko(String v)    { tingkatRisiko = v; }
        public String getSaranTindakan()            { return saranTindakan; }
        public void   setSaranTindakan(String v)    { saranTindakan = v; }
        public String getDisclaimer()               { return disclaimer; }
        public void   setDisclaimer(String v)       { disclaimer = v; }
        public List<String> getKeywords()           { return keywords; }
        public void   setKeywords(List<String> v)   { keywords = v; }
    }
}
