package com.auximad.repository;

import com.auximad.entity.Dossier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DossierRepository extends JpaRepository<Dossier, Integer> {

    Optional<Dossier> findByOrdreTransitId(Integer ordreTransitId);

    Optional<Dossier> findByOrdreTransitIdAndAgenceId(Integer ordreTransitId, Integer agenceId);

    Optional<Dossier> findByNumeroDossier(String numeroDossier);

    /**
     * Chronologie max des numéros compacts et des anciens numéros au format *-NNNN
     */
    @Query(value = """
        SELECT COALESCE(MAX(CAST(RIGHT(numero_dossier, 4) AS INTEGER)), 0)
        FROM dossier
        WHERE numero_dossier ~ '(^[0-9]{5,}$|-[0-9]{4}$)'
        """, nativeQuery = true)
    int findMaxChronologie();

    /** Sérialise la numérotation jusqu'au commit, y compris lorsque la table est vide. */
    @Modifying
    @Query(value = "LOCK TABLE dossier IN SHARE ROW EXCLUSIVE MODE", nativeQuery = true)
    void verrouillerNumerotation();

    /**
     * Recherche paginée et filtrée avec FTS.
     * Paramètres ajoutés :
     *   - agenceId  : si non null, filtre sur id_agence (portée propre agence)
     *   - typeDossier : 'INTERAGENCE' (mere ou sous-dossier), 'MERE' (mere unique), 'SOUS_DOSSIER' (sous-dossier unique) ou null/vide (tous)
     */
    @Query(value = """
        SELECT d.* FROM dossier d
        LEFT JOIN clients c ON d.id_client = c.id
        LEFT JOIN phase_dossier ph ON d.id_phase = ph.id
        LEFT JOIN statut_dossier sd ON d.id_statut = sd.id
        LEFT JOIN ordre_transit ot ON d.id_ordre_transit = ot.id
        WHERE
          (:query IS NULL OR :query = '' OR
            to_tsvector('french', COALESCE(d.numero_dossier, '') || ' ' ||
              COALESCE(c.nom, '') || ' ' || COALESCE(c.prenom, '') || ' ' ||
              COALESCE(d.pour_compte, '') || ' ' || COALESCE(d.depart, ''))
            @@ plainto_tsquery('french', :query)
          )
          AND (:phaseId IS NULL OR d.id_phase = :phaseId)
          AND (:statutId IS NULL OR d.id_statut = :statutId)
          AND (:clientId IS NULL OR d.id_client = :clientId)
          AND (:typeProductionId IS NULL OR d.id_type_production = :typeProductionId)
          AND (:otId IS NULL OR d.id_ordre_transit = :otId)
          AND (:estGroupage IS NULL OR d.est_groupage = :estGroupage)
          AND (:dateMin IS NULL OR d.date_creation >= CAST(:dateMin AS TIMESTAMPTZ))
          AND (:dateMax IS NULL OR d.date_creation <= CAST(:dateMax AS TIMESTAMPTZ))
          AND (:agenceId IS NULL OR d.id_agence = :agenceId)
          AND (
            :typeDossier IS NULL OR :typeDossier = '' OR
            (:typeDossier = 'INTERAGENCE' AND (
               EXISTS (SELECT 1 FROM dossier_interagence di WHERE di.id_sous_dossier = d.id) OR
               EXISTS (SELECT 1 FROM dossier_interagence di WHERE di.id_dossier_mere = d.id)
            )) OR
            (:typeDossier = 'MERE' AND EXISTS (SELECT 1 FROM dossier_interagence di WHERE di.id_dossier_mere = d.id)) OR
            (:typeDossier = 'SOUS_DOSSIER' AND EXISTS (SELECT 1 FROM dossier_interagence di WHERE di.id_sous_dossier = d.id))
          )
        """,
        countQuery = """
        SELECT COUNT(*) FROM dossier d
        LEFT JOIN clients c ON d.id_client = c.id
        WHERE
          (:query IS NULL OR :query = '' OR
            to_tsvector('french', COALESCE(d.numero_dossier, '') || ' ' ||
              COALESCE(c.nom, '') || ' ' || COALESCE(c.prenom, '') || ' ' ||
              COALESCE(d.pour_compte, '') || ' ' || COALESCE(d.depart, ''))
            @@ plainto_tsquery('french', :query)
          )
          AND (:phaseId IS NULL OR d.id_phase = :phaseId)
          AND (:statutId IS NULL OR d.id_statut = :statutId)
          AND (:clientId IS NULL OR d.id_client = :clientId)
          AND (:typeProductionId IS NULL OR d.id_type_production = :typeProductionId)
          AND (:otId IS NULL OR d.id_ordre_transit = :otId)
          AND (:estGroupage IS NULL OR d.est_groupage = :estGroupage)
          AND (:dateMin IS NULL OR d.date_creation >= CAST(:dateMin AS TIMESTAMPTZ))
          AND (:dateMax IS NULL OR d.date_creation <= CAST(:dateMax AS TIMESTAMPTZ))
          AND (:agenceId IS NULL OR d.id_agence = :agenceId)
          AND (
            :typeDossier IS NULL OR :typeDossier = '' OR
            (:typeDossier = 'INTERAGENCE' AND (
               EXISTS (SELECT 1 FROM dossier_interagence di WHERE di.id_sous_dossier = d.id) OR
               EXISTS (SELECT 1 FROM dossier_interagence di WHERE di.id_dossier_mere = d.id)
            )) OR
            (:typeDossier = 'MERE' AND EXISTS (SELECT 1 FROM dossier_interagence di WHERE di.id_dossier_mere = d.id)) OR
            (:typeDossier = 'SOUS_DOSSIER' AND EXISTS (SELECT 1 FROM dossier_interagence di WHERE di.id_sous_dossier = d.id))
          )
        """,
        nativeQuery = true)
    Page<Dossier> searchDossiers(
            @Param("query") String query,
            @Param("phaseId") Integer phaseId,
            @Param("statutId") Integer statutId,
            @Param("clientId") Integer clientId,
            @Param("typeProductionId") Integer typeProductionId,
            @Param("otId") Integer otId,
            @Param("estGroupage") Boolean estGroupage,
            @Param("dateMin") String dateMin,
            @Param("dateMax") String dateMax,
            @Param("agenceId") Integer agenceId,
            @Param("typeDossier") String typeDossier,
            Pageable pageable
    );
    /**
     * Dossiers qui ont des types de documents obligatoires non remis (actifs)
     */
    @Query(value = """
        SELECT DISTINCT d.* FROM dossier d
        JOIN type_production tp ON d.id_type_production = tp.id
        JOIN type_document td ON td.id_type_production = tp.id AND td.est_obligatoire = true
        WHERE NOT EXISTS (
            SELECT 1 FROM document doc
            WHERE doc.id_dossier = d.id
              AND doc.id_type_document = td.id
              AND doc.est_actif = true
        )
        AND (:agenceId IS NULL OR d.id_agence = :agenceId)
        ORDER BY d.date_creation DESC
        LIMIT 50
        """, nativeQuery = true)
    List<Dossier> findDossiersSansDocumentsComplets(@Param("agenceId") Integer agenceId);

    // ─────────── Agrégats pour le RAG (tableau de bord, statistiques) ───────────

    /**
     * Répartition des dossiers par phase.
     * @return couples [nom de la phase, nombre de dossiers]
     */
    @Query(value = """
        SELECT COALESCE(ph.nom, '(sans phase)'), COUNT(*)
        FROM dossier d
        LEFT JOIN phase_dossier ph ON d.id_phase = ph.id
        WHERE (:agenceId IS NULL OR d.id_agence = :agenceId)
        GROUP BY ph.nom
        ORDER BY COUNT(*) DESC
        """, nativeQuery = true)
    List<Object[]> compterDossiersParPhase(@Param("agenceId") Integer agenceId);

    /** Répartition des dossiers par statut. */
    @Query(value = """
        SELECT COALESCE(sd.nom, '(sans statut)'), COUNT(*)
        FROM dossier d
        LEFT JOIN statut_dossier sd ON d.id_statut = sd.id
        WHERE (:agenceId IS NULL OR d.id_agence = :agenceId)
        GROUP BY sd.nom
        ORDER BY COUNT(*) DESC
        """, nativeQuery = true)
    List<Object[]> compterDossiersParStatut(@Param("agenceId") Integer agenceId);

    /** Répartition des dossiers par agence. */
    @Query(value = """
        SELECT a.nom, COUNT(*)
        FROM dossier d
        JOIN agence a ON d.id_agence = a.id
        GROUP BY a.nom
        ORDER BY COUNT(*) DESC
        """, nativeQuery = true)
    List<Object[]> compterDossiersParAgence();

    /** Nombre de dossiers créés depuis une date (format ISO 'yyyy-MM-dd HH:mm:ss'). */
    @Query(value = """
        SELECT COUNT(*) FROM dossier d
        WHERE (:agenceId IS NULL OR d.id_agence = :agenceId)
          AND d.date_creation >= CAST(:debut AS TIMESTAMPTZ)
        """, nativeQuery = true)
    long compterDossiersDepuis(@Param("agenceId") Integer agenceId, @Param("debut") String debut);

    /** Nombre de dossiers dont un document obligatoire manque encore. */
    @Query(value = """
        SELECT COUNT(*) FROM (
            SELECT DISTINCT d.id FROM dossier d
            JOIN type_production tp ON d.id_type_production = tp.id
            JOIN type_document td ON td.id_type_production = tp.id AND td.est_obligatoire = true
            WHERE (:agenceId IS NULL OR d.id_agence = :agenceId)
              AND NOT EXISTS (
                SELECT 1 FROM document doc
                WHERE doc.id_dossier = d.id AND doc.id_type_document = td.id AND doc.est_actif = true
              )
        ) manquants
        """, nativeQuery = true)
    long compterDossiersIncomplets(@Param("agenceId") Integer agenceId);

}
