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

@Service
@Transactional
public class ConteneurService {

    private final ConteneurRepository conteneurRepository;
    private final TypeContainerRepository typeContainerRepository;
    private final DossierRepository dossierRepository;

    public ConteneurService(ConteneurRepository conteneurRepository,
                            TypeContainerRepository typeContainerRepository,
                            DossierRepository dossierRepository) {
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
        if (conteneur.getDossier() == null || conteneur.getDossier().getId() == null) {
            throw new IllegalArgumentException("Le dossier associé est obligatoire pour assigner un conteneur.");
        }

        Dossier dossier = dossierRepository.findById(conteneur.getDossier().getId())
                .orElseThrow(() -> new IllegalArgumentException("Dossier introuvable (ID: " + conteneur.getDossier().getId() + ")"));

        // Vérification du mode de transport / éligibilité conteneur
        if (!estEligibleConteneur(dossier)) {
            throw new IllegalArgumentException("Impossible d'assigner un conteneur : le dossier n'est pas de type maritime.");
        }

        // Validation du numéro de conteneur
        if (conteneur.getNumeroContainer() == null || conteneur.getNumeroContainer().trim().isEmpty()) {
            throw new IllegalArgumentException("Le numéro du conteneur est obligatoire.");
        }

        String numeroNorm = conteneur.getNumeroContainer().trim();
        conteneur.setNumeroContainer(numeroNorm);

        if (conteneur.getId() == null) {
            if (conteneurRepository.existsByNumeroContainer(numeroNorm)) {
                throw new IllegalArgumentException("Le numéro de conteneur '" + numeroNorm + "' existe déjà.");
            }
        } else {
            if (conteneurRepository.existsByNumeroContainerAndIdNot(numeroNorm, conteneur.getId())) {
                throw new IllegalArgumentException("Le numéro de conteneur '" + numeroNorm + "' est déjà utilisé par un autre conteneur.");
            }
        }

        conteneur.setDossier(dossier);
        if (conteneur.getTypeContainer() != null && conteneur.getTypeContainer().getId() != null) {
            TypeContainer tc = typeContainerRepository.findById(conteneur.getTypeContainer().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Type de conteneur introuvable."));
            conteneur.setTypeContainer(tc);
        }

        return conteneurRepository.save(conteneur);
    }

    public void deleteConteneur(Integer id) {
        if (!conteneurRepository.existsById(id)) {
            throw new IllegalArgumentException("Conteneur introuvable.");
        }
        conteneurRepository.deleteById(id);
    }

    /**
     * Vérifie si un dossier est éligible à l'assignation de conteneur (Maritime ou combine ou produit nécessitant conteneur).
     */
    public boolean estEligibleConteneur(Dossier dossier) {
        if (dossier == null) return false;

        // 1. Vérification par typeProduction -> modeTransport
        if (dossier.getTypeProduction() != null) {
            ModeTransport mt = dossier.getTypeProduction().getModeTransport();
            if (mt != null && mt.getNom() != null) {
                String modeNom = mt.getNom().trim();
                if ("Maritime".equalsIgnoreCase(modeNom) || "combine".equalsIgnoreCase(modeNom)) {
                    return true;
                }
            }
            if (dossier.getTypeProduction().getNom() != null
                    && dossier.getTypeProduction().getNom().toLowerCase().contains("maritime")) {
                return true;
            }
        }

        // 2. Vérification par Ordre de Transit -> typeProduction -> modeTransport
        if (dossier.getOrdreTransit() != null && dossier.getOrdreTransit().getTypeProduction() != null) {
            ModeTransport mt = dossier.getOrdreTransit().getTypeProduction().getModeTransport();
            if (mt != null && mt.getNom() != null) {
                String modeNom = mt.getNom().trim();
                if ("Maritime".equalsIgnoreCase(modeNom) || "combine".equalsIgnoreCase(modeNom)) {
                    return true;
                }
            }
        }

        return false;
    }
}
