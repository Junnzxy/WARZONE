package alp.semester2.tugas.controller;

import alp.semester2.tugas.model.Disease;
import alp.semester2.tugas.repository.DiseaseRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * DiseaseController
 * -----------------------------------------------
 * GET /api/diseases              → semua penyakit
 * GET /api/diseases/{id}         → detail satu penyakit
 * GET /api/diseases/search?q=    → cari berdasarkan keyword
 * GET /api/diseases/kategori?k=  → filter berdasarkan kategori
 *
 * Endpoint ini public (tidak butuh login) —
 * dipakai frontend untuk halaman edukasi.
 */
@RestController
@RequestMapping("/api/diseases")
public class DiseaseController {

    private final DiseaseRepository diseaseRepo;

    public DiseaseController(DiseaseRepository diseaseRepo) {
        this.diseaseRepo = diseaseRepo;
    }

    @GetMapping
    public ResponseEntity<List<Disease>> getAll() {
        return ResponseEntity.ok(diseaseRepo.findAllByOrderByNamaPenyakitAsc());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Disease> getById(@PathVariable Long id) {
        return diseaseRepo.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Disease>> search(@RequestParam("q") String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(diseaseRepo.findByKeyword(keyword.trim()));
    }

    @GetMapping("/kategori")
    public ResponseEntity<List<Disease>> byKategori(@RequestParam("k") String kategori) {
        return ResponseEntity.ok(
            diseaseRepo.findByKategoriContainingIgnoreCase(kategori)
        );
    }

    @GetMapping("/risiko/{level}")
    public ResponseEntity<List<Disease>> byRisiko(@PathVariable String level) {
        try {
            Disease.TingkatRisiko risiko = Disease.TingkatRisiko.valueOf(level.toLowerCase());
            return ResponseEntity.ok(diseaseRepo.findByTingkatRisiko(risiko));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
