package com.auximad.repository;

import com.auximad.entity.DossierInteragence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DossierInteragenceRepository extends JpaRepository<DossierInteragence, Integer> {

    /** Tous les sous-dossiers d'un dossier mère */
    List<DossierInteragence> findByDossierMereId(Integer dossierMereId);

    /** Premier lien créé pour ce sous-dossier, même si des liens en double existent déjà. */
    Optional<DossierInteragence> findFirstBySousDossierIdOrderByIdAsc(Integer sousDossierId);

    /** Vérifie si un dossier est déjà sous-dossier d'un autre */
    boolean existsBySousDossierId(Integer sousDossierId);
}
