package alp.semester2.tugas.repository;

import alp.semester2.tugas.model.Disease;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiseaseRepository extends JpaRepository<Disease, Long> {

    // Cari penyakit berdasarkan keyword (nama atau medical term)
    // Digunakan untuk mencocokkan hasil Gemini ke database
    @Query("SELECT d FROM Disease d WHERE " +
           "LOWER(d.namaPenyakit) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(d.medicalTerm)  LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Disease> findByKeyword(@Param("keyword") String keyword);

    // Filter berdasarkan kategori
    List<Disease> findByKategoriContainingIgnoreCase(String kategori);

    // Filter berdasarkan tingkat risiko
    List<Disease> findByTingkatRisiko(Disease.TingkatRisiko tingkatRisiko);

    // Semua penyakit diurutkan berdasarkan nama
    List<Disease> findAllByOrderByNamaPenyakitAsc();
}
