package com.auximad.service;

import com.auximad.entity.Conteneur;
import com.auximad.entity.Dossier;
import com.auximad.entity.ModeTransport;
import com.auximad.entity.TypeContainer;
import com.auximad.repository.ConteneurRepository;
import com.auximad.repository.DossierRepository;
import com.auximad.repository.TypeContainerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.HashSet;
import java.util.Set;

@Service
@Transactional
public class ConteneurService {

    private final SuppressionService suppressionService;

    private final ConteneurRepository conteneurRepository;
    private final TypeContainerRepository typeContainerRepository;
    private final DossierRepository dossierRepository;

    public ConteneurService(ConteneurRepository conteneurRepository,
                            TypeContainerRepository typeContainerRepository,
                            DossierRepository dossierRepository, SuppressionService suppressionService) {
        this.suppressionService = suppressionService;
        this.conteneurRepository = conteneurRepository;
        this.typeContainerRepository = typeContainerRepository;
        this.dossierRepository = dossierRepository;
    }

    public List<TypeContainer> getAllTypeContainers() {
        return typeContainerRepository.findAll();
    }

    public List<Conteneur> getConteneursByDossierId(Integer dossierId) {
        return conteneurRepository.findByDossierId(dossierId);
    }

    public List<Conteneur> getAllConteneurs() {
        return conteneurRepository.findAll();
    }

    public Optional<Conteneur> getById(Integer id) {
        return conteneurRepository.findById(id);
    }

    public Conteneur saveConteneur(Conteneur conteneur) {
        Set<Integer> dossierIds = new HashSet<>();
        if (conteneur.getDossiers() != null) {
            for (Dossier source : conteneur.getDossiers()) {
                if (source == null || source.getId() == null) {
                    throw new IllegalArgumentException("Le dossier associé est obligatoire pour assigner un conteneur.");
                }
                dossierIds.add(source.getId());
            }
        }
        if (dossierIds.isEmpty()) {
            throw new IllegalArgumentException("Le dossier associé est obligatoire pour assigner un conteneur.");
        }
        if (dossierIds.size() != 1) {
            throw new IllegalArgumentException("Une assignation doit désigner un seul dossier.");
        }
        final Integer dossierIdTarget = dossierIds.iterator().next();
        Dossier dossier = dossierRepository.findById(dossierIdTarget)
                .orElseThrow(() -> new IllegalArgumentException("Dossier introuvable (ID: " + dossierIdTarget + ")"));

        // Vérification du mode de transport / éligibilité conteneur
        if (!estEligibleConteneur(dossier)) {
            throw new IllegalArgumentException("Impossible d'assigner un conteneur : le dossier n'est pas de type maritime.");
        }

        // Validation du numéro de conteneur
        if (conteneur.getNumeroContainer() == null || conteneur.getNumeroContainer().trim().isEmpty()) {
            throw new IllegalArgumentException("Le numéro du conteneur est obligatoire.");
        }

        String numeroNorm = conteneur.getNumeroContainer().trim();

        TypeContainer tc = null;
        if (conteneur.getTypeContainer() != null && conteneur.getTypeContainer().getId() != null) {
            tc = typeContainerRepository.findById(conteneur.getTypeContainer().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Type de conteneur introuvable."));
        }

        // Si le conteneur existe déjà (par numéro), réutiliser/mettre à jour l'enregistrement existant
        Optional<Conteneur> existantOpt = conteneurRepository.findByNumeroContainer(numeroNorm);
        Conteneur cTarget;

        if (conteneur.getId() != null) {
            cTarget = conteneurRepository.findById(conteneur.getId())
                    .orElseGet(() -> existantOpt.orElse(new Conteneur()));
        } else if (existantOpt.isPresent()) {
            cTarget = existantOpt.get();
        } else {
            cTarget = new Conteneur();
        }

        cTarget.setNumeroContainer(numeroNorm);
        if (tc != null) {
            cTarget.setTypeContainer(tc);
        }
        if (conteneur.getDescription() != null) {
            cTarget.setDescription(conteneur.getDescription());
        }

        // Ajout de l'association au dossier courant (sans supprimer les associations existantes)
        cTarget.getDossiers().add(dossier);

        return conteneurRepository.save(cTarget);
    }

    public void deleteConteneur(Integer conteneurId, Integer dossierId) {
        if (dossierId != null) {
            suppressionService.retirerAssignationConteneur(conteneurId, dossierId);
        } else {
            suppressionService.supprimer("conteneur", conteneurId);
        }
    }

    /**
     * L'assignation d'un conteneur est réservée au transport maritime.
     */
    public boolean estEligibleConteneur(Dossier dossier) {
        if (dossier == null) return false;

        // Le mode renseigné sur le dossier est prioritaire sur celui de l'OT.
        if (dossier.getTypeProduction() != null) {
            ModeTransport mt = dossier.getTypeProduction().getModeTransport();
            if (mt != null && mt.getNom() != null && !mt.getNom().isBlank()) {
                return "Maritime".equalsIgnoreCase(mt.getNom().trim());
            }
        }

        // 2. Vérification par Ordre de Transit -> typeProduction -> modeTransport
        if (dossier.getOrdreTransit() != null && dossier.getOrdreTransit().getTypeProduction() != null) {
            ModeTransport mt = dossier.getOrdreTransit().getTypeProduction().getModeTransport();
            if (mt != null && mt.getNom() != null) {
                return "Maritime".equalsIgnoreCase(mt.getNom().trim());
            }
        }

        return false;
    }
}
