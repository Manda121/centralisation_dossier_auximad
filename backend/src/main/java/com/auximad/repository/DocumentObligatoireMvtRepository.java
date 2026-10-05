package com.auximad.repository;

import com.auximad.entity.DocumentObligatoireMvt;
import com.auximad.entity.TypeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentObligatoireMvtRepository extends JpaRepository<DocumentObligatoireMvt, Integer> {

    /**
     * Compte les types de documents obligatoires (table document_obligatoire_mvt)
     * non encore remis pour un dossier : aucun document actif ne correspond.
     * Un type de document sans type de production s'applique a tous les dossiers.
     */
    @Query("""
        SELECT COUNT(dov) FROM DocumentObligatoireMvt dov
        JOIN dov.typeDocument td
        LEFT JOIN td.typeProduction tp
        WHERE (tp IS NULL OR tp.id = :typeProductionId)
          AND NOT EXISTS (
            SELECT 1 FROM Document d
            WHERE d.dossier.id = :dossierId
              AND d.typeDocument.id = td.id
              AND d.estActif = true
          )
    """)
    long countManquants(@Param("dossierId") Integer dossierId,
                        @Param("typeProductionId") Integer typeProductionId);

    /**
     * Liste des types de documents obligatoires manquants pour un dossier
     * (utilisee par la fenetre d'assignation de document).
     */
    @Query("""
        SELECT td FROM DocumentObligatoireMvt dov
        JOIN dov.typeDocument td
        LEFT JOIN td.typeProduction tp
        WHERE (tp IS NULL OR tp.id = :typeProductionId)
          AND NOT EXISTS (
            SELECT 1 FROM Document d
            WHERE d.dossier.id = :dossierId
              AND d.typeDocument.id = td.id
              AND d.estActif = true
          )
        ORDER BY td.nom
    """)
    List<TypeDocument> findManquants(@Param("dossierId") Integer dossierId,
                                     @Param("typeProductionId") Integer typeProductionId);
}
