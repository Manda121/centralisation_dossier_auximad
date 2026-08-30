package com.auximad.service;

import com.auximad.dto.SousDossierRequest;
import com.auximad.entity.*;
import com.auximad.repository.*;
import com.auximad.security.AutorisationHelper;
import com.auximad.security.JwtSecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class InterAgenceService {

    private final DossierInteragenceRepository interagenceRepository;
    private final DossierRepository dossierRepository;
    private final AgenceRepository agenceRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final DossierService dossierService;

    public InterAgenceService(DossierInteragenceRepository interagenceRepository,
                               DossierRepository dossierRepository,
                               AgenceRepository agenceRepository,
                               UtilisateurRepository utilisateurRepository,
                               DossierService dossierService) {
        this.interagenceRepository = interagenceRepository;
        this.dossierRepository = dossierRepository;
        this.agenceRepository = agenceRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.dossierService = dossierService;
    }

    /**
     * Crée un sous-dossier inter-agences à partir d'un dossier mère.
     * L'utilisateur connecté doit avoir ACTION_SOUS_DOSSIER en écriture (niveau >= 220).
     *
     * @param idDossierMere ID du dossier mère
     * @param request       Données du sous-dossier (agence cible, motif)
     * @return Le DossierInteragence créé (avec le sous-dossier lié)
     */
    @Transactional
    public DossierInteragence creerSousDossier(Integer idDossierMere, SousDossierRequest request) {
        // 1. Vérifier permission
        JwtSecurityContext.aPermission(
                AutorisationHelper.PORTEE_PROPRE_ECRITURE,
                AutorisationHelper.ACTION_SOUS_DOSSIER
        );

        // 2. Charger le dossier mère
        Dossier mere = dossierRepository.findById(idDossierMere)
                .orElseThrow(() -> new IllegalArgumentException("Dossier mère introuvable : " + idDossierMere));

        // 3. Charger l'agence cible
        Agence agenceCible = agenceRepository.findById(request.getIdAgence())
                .orElseThrow(() -> new IllegalArgumentException("Agence introuvable : " + request.getIdAgence()));

        // 4. Récupérer l'utilisateur connecté
        Integer idUtilisateur = JwtSecurityContext.getIdUtilisateur();
        Utilisateur utilisateur = utilisateurRepository.findById(idUtilisateur)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur connecté introuvable."));

        // 5. Construire le sous-dossier en héritant du dossier mère
        Dossier sousDossier = new Dossier();
        sousDossier.setAgence(agenceCible);
        sousDossier.setUtilisateur(utilisateur);
        sousDossier.setClient(mere.getClient());
        sousDossier.setOrdreTransit(mere.getOrdreTransit());
        sousDossier.setEstGroupage(mere.getEstGroupage());
        sousDossier.setPhaseDossier(mere.getPhaseDossier());
        sousDossier.setStatutDossier(mere.getStatutDossier());
        sousDossier.setPaysOrigine(mere.getPaysOrigine());
        sousDossier.setPaysDestination(mere.getPaysDestination());
        sousDossier.setInconterme(mere.getInconterme());
        sousDossier.setEstPrepaye(mere.getEstPrepaye());
        sousDossier.setEstProvisionne(false);
        sousDossier.setProvisionDemandee(java.math.BigDecimal.ZERO);
        sousDossier.setDateCreation(LocalDateTime.now());

        // Choisir le type de production (override possible)
        if (request.getIdTypeProduction() != null) {
            TypeProduction tp = new TypeProduction();
            tp.setId(request.getIdTypeProduction());
            sousDossier.setTypeProduction(tp);
        } else {
            sousDossier.setTypeProduction(mere.getTypeProduction());
        }

        // 6. Sauvegarder le sous-dossier (génère son numéro automatiquement)
        Dossier savedSousDossier = dossierService.saveDossier(sousDossier);

        // 7. Créer le lien interagence
        DossierInteragence lien = new DossierInteragence();
        lien.setDossierMere(mere);
        lien.setSousDossier(savedSousDossier);
        lien.setMotif(request.getMotif());
        lien.setDateCreation(LocalDateTime.now());

        return interagenceRepository.save(lien);
    }

    /**
     * Retourne tous les sous-dossiers d'un dossier mère.
     */
    @Transactional(readOnly = true)
    public List<DossierInteragence> getSousDossiers(Integer idDossierMere) {
        return interagenceRepository.findByDossierMereId(idDossierMere);
    }

    /**
     * Retourne le lien interagence d'un sous-dossier (pour retrouver son dossier mère).
     */
    @Transactional(readOnly = true)
    public Optional<DossierInteragence> getDossierMere(Integer idSousDossier) {
        return interagenceRepository.findBySousDossierId(idSousDossier);
    }
}
