package com.auximad.repository;

import com.auximad.entity.Remise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RemiseRepository extends JpaRepository<Remise, Integer> {

    /** Toutes les remises d'un dossier */
    List<Remise> findByDossierId(Integer dossierId);

    /** Remise d'un dossier pour un type de document donné */
    Optional<Remise> findByDossierIdAndTypeDocumentId(Integer dossierId, Integer typeDocumentId);

    /** Remises urgentes, toutes agences confondues */
    @Query(value = """
        SELECT r.* FROM remise r
        JOIN dossier d ON r.id_dossier = d.id
        WHERE r.date_remise IS NOT NULL
          AND r.date_remise >= :maintenant
          AND r.date_remise <= :limite
        ORDER BY r.date_remise ASC
        """, nativeQuery = true)
    List<Remise> findRemisesUrgentes(@Param("maintenant") LocalDateTime maintenant,
                                     @Param("limite") LocalDateTime limite);

    /** Remises urgentes d'une agence donnée */
    @Query(value = """
        SELECT r.* FROM remise r
        JOIN dossier d ON r.id_dossier = d.id
        WHERE r.date_remise IS NOT NULL
          AND r.date_remise >= :maintenant
          AND r.date_remise <= :limite
          AND d.id_agence = :agenceId
        ORDER BY r.date_remise ASC
        """, nativeQuery = true)
    List<Remise> findRemisesUrgentesParAgence(@Param("maintenant") LocalDateTime maintenant,
                                              @Param("limite") LocalDateTime limite,
                                              @Param("agenceId") Integer agenceId);
}
