package com.auximad.service;

import com.auximad.repository.DossierInteragenceRepository;
import com.auximad.repository.DossierRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/** Reprend aussi les sous-dossiers crees avant la mise a jour du serveur. */
@Service
public class InitialisationAvancementInteragence {
    private final DossierInteragenceRepository liens;
    private final DossierRepository dossiers;
    private final SynchronisationMouvementsService synchronisation;

    public InitialisationAvancementInteragence(DossierInteragenceRepository liens,
                                               DossierRepository dossiers,
                                               SynchronisationMouvementsService synchronisation) {
        this.liens = liens;
        this.dossiers = dossiers;
        this.synchronisation = synchronisation;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void reprendreFamillesExistantes() {
        Set<Integer> dejaTraites = new HashSet<>();
        for (Integer id : liens.findDossierMereIds()) {
            if (dejaTraites.contains(id)) continue;
            synchronisation.alignerDepuisMere(id);
            dejaTraites.addAll(dossiers.findFamilleIds(id));
        }
    }
}
