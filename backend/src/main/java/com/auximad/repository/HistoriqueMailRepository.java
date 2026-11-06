package com.auximad.repository;

import com.auximad.entity.HistoriqueMail;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface HistoriqueMailRepository extends JpaRepository<HistoriqueMail, Long> {
    boolean existsByIdMailOrigine(Long idMailOrigine);

    @Modifying(flushAutomatically = true)
    @Query("update HistoriqueMail m set m.statut = :annule where m.idDemandeInteragence = :demande "
            + "and m.statut = :attente")
    void annulerEnAttente(@Param("demande") Integer demande,
            @Param("attente") HistoriqueMail.Statut attente, @Param("annule") HistoriqueMail.Statut annule);
    @Query("select m from HistoriqueMail m where (:agence is null or m.idAgence = :agence) "
            + "and (:statut is null or m.statut = :statut)")
    Page<HistoriqueMail> rechercher(@Param("agence") Integer agence,
            @Param("statut") HistoriqueMail.Statut statut, Pageable pageable);

    List<HistoriqueMail> findByStatutOrderByIdAsc(HistoriqueMail.Statut statut, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from HistoriqueMail m where m.id = :id")
    Optional<HistoriqueMail> verrouiller(@Param("id") Long id);
}
