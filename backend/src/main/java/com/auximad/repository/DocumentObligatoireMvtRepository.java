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
    @Query(value = """
        SELECT COUNT(*) FROM document_obligatoire_mvt dov
        JOIN type_document td ON td.id = dov.id_type_document
        JOIN dossier dos ON dos.id = :dossierId
        LEFT JOIN ordre_transit ot ON ot.id = dos.id_ordre_transit
        WHERE dov.id_operation = :operationId
          AND (td.id_nature_marchandise IS NULL OR td.id_nature_marchandise = ot.id_nature_marchandise)
          AND NOT EXISTS (SELECT 1 FROM document doc
            WHERE doc.id_dossier = dos.id AND doc.id_type_document = td.id AND doc.est_actif = true)
    """, nativeQuery = true)
    long countManquants(@Param("dossierId") Integer dossierId,
                        @Param("operationId") Integer operationId);

    /**
     * Liste des types de documents obligatoires manquants pour une operation
     * donnee (utilisee par la fenetre d'assignation de document).
     */
    @Query(value = """
        SELECT td.* FROM document_obligatoire_mvt dov
        JOIN type_document td ON td.id = dov.id_type_document
        JOIN dossier dos ON dos.id = :dossierId
        LEFT JOIN ordre_transit ot ON ot.id = dos.id_ordre_transit
        WHERE dov.id_operation = :operationId
          AND (td.id_nature_marchandise IS NULL OR td.id_nature_marchandise = ot.id_nature_marchandise)
          AND NOT EXISTS (SELECT 1 FROM document doc
            WHERE doc.id_dossier = dos.id AND doc.id_type_document = td.id AND doc.est_actif = true)
        ORDER BY td.nom
    """, nativeQuery = true)
    List<TypeDocument> findManquants(@Param("dossierId") Integer dossierId,
                                     @Param("operationId") Integer operationId);

    /**
     * Vue globale (tableau de bord / verification) : compte tous les documents
     * obligatoires non remis pour un dossier, toutes operations confondues
     * du type de production du dossier.
     */
    @Query(value = """
        SELECT COUNT(*) FROM document_obligatoire_mvt dov
        JOIN operation_dossier op ON op.id = dov.id_operation
        JOIN type_document td ON td.id = dov.id_type_document
        JOIN dossier dos ON dos.id = :dossierId
        LEFT JOIN ordre_transit ot ON ot.id = dos.id_ordre_transit
        WHERE op.id_type_production = :typeProductionId
          AND (td.id_nature_marchandise IS NULL OR td.id_nature_marchandise = ot.id_nature_marchandise)
          AND NOT EXISTS (SELECT 1 FROM document doc
            WHERE doc.id_dossier = dos.id AND doc.id_type_document = td.id AND doc.est_actif = true)
    """, nativeQuery = true)
    long countManquantsDossier(@Param("dossierId") Integer dossierId,
                               @Param("typeProductionId") Integer typeProductionId);
}
