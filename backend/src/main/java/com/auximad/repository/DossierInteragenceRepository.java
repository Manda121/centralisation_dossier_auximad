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

    /** Le lien interagence dont ce dossier est le sous-dossier (retrouve son dossier mère) */
    Optional<DossierInteragence> findBySousDossierId(Integer sousDossierId);

    /** Vérifie si un dossier est déjà sous-dossier d'un autre */
    boolean existsBySousDossierId(Integer sousDossierId);
}
