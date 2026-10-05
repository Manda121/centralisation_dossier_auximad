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
     * Compte les documents obligatoires d'une operation (table
     * document_obligatoire_mvt) non encore remis pour un dossier : aucun
     * document actif ne correspond. Une operation sans ligne ici a 0 requis.
     */
    @Query("""
        SELECT COUNT(dov) FROM DocumentObligatoireMvt dov
        WHERE dov.operation.id = :operationId
          AND NOT EXISTS (
            SELECT 1 FROM Document d
            WHERE d.dossier.id = :dossierId
              AND d.typeDocument.id = dov.typeDocument.id
              AND d.estActif = true
          )
    """)
    long countManquants(@Param("dossierId") Integer dossierId,
                        @Param("operationId") Integer operationId);

    /**
     * Liste des types de documents obligatoires manquants pour une operation
     * donnee (utilisee par la fenetre d'assignation de document).
     */
    @Query("""
        SELECT dov.typeDocument FROM DocumentObligatoireMvt dov
        WHERE dov.operation.id = :operationId
          AND NOT EXISTS (
            SELECT 1 FROM Document d
            WHERE d.dossier.id = :dossierId
              AND d.typeDocument.id = dov.typeDocument.id
              AND d.estActif = true
          )
        ORDER BY dov.typeDocument.nom
    """)
    List<TypeDocument> findManquants(@Param("dossierId") Integer dossierId,
                                     @Param("operationId") Integer operationId);

    /**
     * Vue globale (tableau de bord / verification) : compte tous les documents
     * obligatoires non remis pour un dossier, toutes operations confondues
     * du type de production du dossier.
     */
    @Query("""
        SELECT COUNT(dov) FROM DocumentObligatoireMvt dov
        JOIN dov.operation op
        WHERE op.typeProduction.id = :typeProductionId
          AND NOT EXISTS (
            SELECT 1 FROM Document d
            WHERE d.dossier.id = :dossierId
              AND d.typeDocument.id = dov.typeDocument.id
              AND d.estActif = true
          )
    """)
    long countManquantsDossier(@Param("dossierId") Integer dossierId,
                               @Param("typeProductionId") Integer typeProductionId);
}
