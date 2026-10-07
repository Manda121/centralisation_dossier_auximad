package com.auximad.repository;

import com.auximad.entity.MvtDossier;
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
public interface MvtDossierRepository extends JpaRepository<MvtDossier, Integer> {

    List<MvtDossier> findByDossierIdOrderByDateMvtAscIdAsc(Integer dossierId);

    List<MvtDossier> findByDossierIdAndEstAnnuleFalseOrderByIdAsc(Integer dossierId);

    Optional<MvtDossier> findFirstByDossierIdAndOperationIdOrderByDateMvtDescIdDesc(Integer dossierId, Integer operationId);

    @Query(value = """
        SELECT m FROM MvtDossier m
        LEFT JOIN m.dossier d
        LEFT JOIN d.client c
        LEFT JOIN d.agence a
        LEFT JOIN m.utilisateur u
        LEFT JOIN m.operation op
        LEFT JOIN d.typeProduction tp
        WHERE (:query IS NULL OR :query = ''
               OR LOWER(COALESCE(d.numeroDossier, '')) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(COALESCE(c.cpClient, '')) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(COALESCE(c.nom, '')) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(COALESCE(op.description, '')) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(COALESCE(u.nomUtilisateur, '')) LIKE LOWER(CONCAT('%', :query, '%')))
          AND (CAST(:dossierId AS integer) IS NULL OR d.id = :dossierId)
          AND (:numeroDossier IS NULL OR :numeroDossier = '' OR LOWER(d.numeroDossier) LIKE LOWER(CONCAT('%', :numeroDossier, '%')))
          AND (:cpClient IS NULL OR :cpClient = '' OR LOWER(c.cpClient) LIKE LOWER(CONCAT('%', :cpClient, '%')))
          AND (CAST(:agenceId AS integer) IS NULL OR a.id = :agenceId)
          AND (CAST(:typeProductionId AS integer) IS NULL OR tp.id = :typeProductionId)
          AND (CAST(:phaseId AS integer) IS NULL OR m.nouvellePhase.id = :phaseId)
          AND (CAST(:statutId AS integer) IS NULL OR m.nouveauStatut.id = :statutId)
          AND (CAST(:operationId AS integer) IS NULL OR op.id = :operationId)
          AND (CAST(:utilisateurId AS integer) IS NULL OR u.id = :utilisateurId)
          AND (CAST(:dateMin AS timestamp) IS NULL OR m.dateMvt >= :dateMin)
          AND (CAST(:dateMax AS timestamp) IS NULL OR m.dateMvt <= :dateMax)
        """)
    Page<MvtDossier> searchMouvements(
            @Param("query") String query,
            @Param("dossierId") Integer dossierId,
            @Param("numeroDossier") String numeroDossier,
            @Param("cpClient") String cpClient,
            @Param("agenceId") Integer agenceId,
            @Param("typeProductionId") Integer typeProductionId,
            @Param("phaseId") Integer phaseId,
            @Param("statutId") Integer statutId,
            @Param("operationId") Integer operationId,
            @Param("utilisateurId") Integer utilisateurId,
            @Param("dateMin") LocalDateTime dateMin,
            @Param("dateMax") LocalDateTime dateMax,
            Pageable pageable
    );
}
