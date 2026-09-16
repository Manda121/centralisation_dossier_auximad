package com.auximad.repository;

import com.auximad.entity.DemandeInteragence;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DemandeInteragenceRepository extends JpaRepository<DemandeInteragence, Integer> {

    List<DemandeInteragence> findByDossierIdOrderByDateDemandeDesc(Integer dossierId);

    /** Supprimer toutes les demandes d'inter-agence associées à un dossier */
    void deleteByDossierId(Integer dossierId);

    Optional<DemandeInteragence> findFirstByDossierIdAndStatutOrderByDateDemandeDesc(Integer dossierId, String statut);

    long countByAgenceDestinataireIdAndStatutAndEstVueFalse(Integer agenceDestinataireId, String statut);

    @Query(value = """
        SELECT d.* FROM demande_interagence d
        LEFT JOIN dossier dos ON d.id_dossier = dos.id
        LEFT JOIN clients c ON dos.id_client = c.id
        WHERE
          (:dateMin IS NULL OR :dateMin = '' OR d.date_demande >= CAST(:dateMin AS TIMESTAMPTZ))
          AND (:dateMax IS NULL OR :dateMax = '' OR d.date_demande <= CAST(:dateMax AS TIMESTAMPTZ))
          AND (:statut IS NULL OR :statut = '' OR LOWER(d.statut) = LOWER(:statut))
          AND (:agenceDemandeuseId IS NULL OR d.id_agence_demandeuse = :agenceDemandeuseId)
          AND (:agenceDestinataireId IS NULL OR d.id_agence_destinataire = :agenceDestinataireId)
          AND (:agenceConcerneeId IS NULL OR d.id_agence_demandeuse = :agenceConcerneeId OR d.id_agence_destinataire = :agenceConcerneeId)
          AND (:query IS NULL OR :query = '' OR
               LOWER(COALESCE(dos.numero_dossier, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(COALESCE(d.motif, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(COALESCE(c.nom, '')) LIKE LOWER(CONCAT('%', :query, '%'))
              )
        """,
        countQuery = """
        SELECT COUNT(*) FROM demande_interagence d
        LEFT JOIN dossier dos ON d.id_dossier = dos.id
        LEFT JOIN clients c ON dos.id_client = c.id
        WHERE
          (:dateMin IS NULL OR :dateMin = '' OR d.date_demande >= CAST(:dateMin AS TIMESTAMPTZ))
          AND (:dateMax IS NULL OR :dateMax = '' OR d.date_demande <= CAST(:dateMax AS TIMESTAMPTZ))
          AND (:statut IS NULL OR :statut = '' OR LOWER(d.statut) = LOWER(:statut))
          AND (:agenceDemandeuseId IS NULL OR d.id_agence_demandeuse = :agenceDemandeuseId)
          AND (:agenceDestinataireId IS NULL OR d.id_agence_destinataire = :agenceDestinataireId)
          AND (:agenceConcerneeId IS NULL OR d.id_agence_demandeuse = :agenceConcerneeId OR d.id_agence_destinataire = :agenceConcerneeId)
          AND (:query IS NULL OR :query = '' OR
               LOWER(COALESCE(dos.numero_dossier, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(COALESCE(d.motif, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(COALESCE(c.nom, '')) LIKE LOWER(CONCAT('%', :query, '%'))
              )
        """,
        nativeQuery = true)
    Page<DemandeInteragence> searchDemandes(
            @Param("dateMin") String dateMin,
            @Param("dateMax") String dateMax,
            @Param("statut") String statut,
            @Param("agenceDemandeuseId") Integer agenceDemandeuseId,
            @Param("agenceDestinataireId") Integer agenceDestinataireId,
            @Param("agenceConcerneeId") Integer agenceConcerneeId,
            @Param("query") String query,
            Pageable pageable
    );
}
