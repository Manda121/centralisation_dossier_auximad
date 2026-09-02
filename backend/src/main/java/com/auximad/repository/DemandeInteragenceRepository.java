package com.auximad.repository;

import com.auximad.entity.DemandeInteragence;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DemandeInteragenceRepository extends JpaRepository<DemandeInteragence, Integer> {

    List<DemandeInteragence> findByDossierIdOrderByDateDemandeDesc(Integer dossierId);

    Optional<DemandeInteragence> findFirstByDossierIdAndStatutOrderByDateDemandeDesc(Integer dossierId, String statut);

    long countByAgenceDestinataireIdAndStatutAndEstVueFalse(Integer agenceDestinataireId, String statut);

    @Query("SELECT d FROM DemandeInteragence d WHERE " +
           "(:dateMin IS NULL OR d.dateDemande >= :dateMin) AND " +
           "(:dateMax IS NULL OR d.dateDemande <= :dateMax) AND " +
           "(:statut IS NULL OR LOWER(d.statut) = LOWER(:statut)) AND " +
           "(:agenceDemandeuseId IS NULL OR d.agenceDemandeuse.id = :agenceDemandeuseId) AND " +
           "(:agenceDestinataireId IS NULL OR d.agenceDestinataire.id = :agenceDestinataireId) AND " +
           "(:agenceConcerneeId IS NULL OR d.agenceDemandeuse.id = :agenceConcerneeId OR d.agenceDestinataire.id = :agenceConcerneeId) AND " +
           "(:query IS NULL OR LOWER(d.dossier.numeroDossier) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(d.motif) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(d.dossier.client.nom) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<DemandeInteragence> searchDemandes(
            @Param("dateMin") LocalDateTime dateMin,
            @Param("dateMax") LocalDateTime dateMax,
            @Param("statut") String statut,
            @Param("agenceDemandeuseId") Integer agenceDemandeuseId,
            @Param("agenceDestinataireId") Integer agenceDestinataireId,
            @Param("agenceConcerneeId") Integer agenceConcerneeId,
            @Param("query") String query,
            Pageable pageable
    );
}
