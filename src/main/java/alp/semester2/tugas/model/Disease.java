package alp.semester2.tugas.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Disease
 * -----------------------------------------------
 * Entity ini merepresentasikan satu baris dari
 * database Google Sheet kalian:
 *
 *  id | nama_penyakit | medical_term | kategori |
 *  tingkat_risiko | solusi_edukasi |
 *  url_nejm | url_mayo_clinic | url_pubmed
 */
@Entity
@Table(name = "diseases", indexes = {
    @Index(name = "idx_kategori",      columnList = "kategori"),
    @Index(name = "idx_tingkat_risiko",columnList = "tingkat_risiko")
})
@Data
@NoArgsConstructor
public class Disease {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Kolom B: Nama Penyakit (ID)
    @Column(name = "nama_penyakit", nullable = false, length = 200)
    private String namaPenyakit;

    // Kolom C: Medical Term (EN)
    @Column(name = "medical_term", nullable = false, length = 200)
    private String medicalTerm;

    // Kolom D: Kategori
    @Column(nullable = false, length = 100)
    private String kategori;

    // Kolom H: Tingkat Risiko
    @Enumerated(EnumType.STRING)
    @Column(name = "tingkat_risiko", nullable = false, length = 10)
    private TingkatRisiko tingkatRisiko;

    // Kolom I: Solusi Edukasi Penanganan (isi dari tim kalian, bukan AI)
    @Column(name = "solusi_edukasi", nullable = false, columnDefinition = "TEXT")
    private String solusiEdukasi;

    // Kolom E: Jurnal Internasional (NEJM)
    @Column(name = "url_nejm", length = 500)
    private String urlNejm;

    // Kolom F: Artikel Medis (Mayo Clinic)
    @Column(name = "url_mayo_clinic", length = 500)
    private String urlMayoClinic;

    // Kolom G: Paper Ilmiah (PubMed)
    @Column(name = "url_pubmed", length = 500)
    private String urlPubmed;

    public enum TingkatRisiko {
        ringan, sedang, berat
    }
}
