package com.auximad.repository;

import com.auximad.entity.Dossier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DossierRepository extends JpaRepository<Dossier, Integer> {

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
            Pageable pageable
    );
}
