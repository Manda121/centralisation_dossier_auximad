package com.auximad.service;

import com.auximad.entity.Conteneur;
import com.auximad.entity.Document;
import com.auximad.entity.Dossier;
import com.auximad.entity.Remise;
import com.auximad.repository.ConteneurRepository;
import com.auximad.repository.DocumentRepository;
import com.auximad.repository.RemiseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Reprend les ressources disponibles lors de la creation du sous-dossier. */
@Service
public class HeritageSousDossierService {
    private final ConteneurRepository conteneurs;
    private final DocumentRepository documents;
    private final RemiseRepository remises;
    private final ConteneurService conteneurService;

    public HeritageSousDossierService(ConteneurRepository conteneurs, DocumentRepository documents,
                                      RemiseRepository remises, ConteneurService conteneurService) {
        this.conteneurs = conteneurs;
        this.documents = documents;
        this.remises = remises;
        this.conteneurService = conteneurService;
    }

    @Transactional
    public void copierRessources(Dossier mere, Dossier sousDossier) {
        List<Conteneur> partages = conteneurs.findByDossierId(mere.getId());
        if (!partages.isEmpty() && !conteneurService.estEligibleConteneur(sousDossier)) {
            throw new IllegalArgumentException(
                    "Le sous-dossier doit etre maritime pour reprendre les conteneurs du dossier mere.");
        }
        for (Conteneur conteneur : partages) {
            // Meme conteneur physique, une association supplementaire au sous-dossier.
            conteneur.getDossiers().add(sousDossier);
        }
        conteneurs.saveAll(partages);

        List<Document> copies = documents.findByDossierIdAndEstActifTrue(mere.getId()).stream().map(source -> {
            Document copie = new Document();
            copie.setDossier(sousDossier);
            copie.setTypeDocument(source.getTypeDocument());
            copie.setNombre(source.getNombre());
            copie.setCheminFichier(source.getCheminFichier());
            copie.setNomFichier(source.getNomFichier());
            copie.setEstActif(true);
            copie.setDateAjout(source.getDateAjout());
            return copie;
        }).toList();
        documents.saveAll(copies);

        List<Remise> copiesRemises = remises.findByDossierId(mere.getId()).stream().map(source -> {
            Remise copie = new Remise();
            copie.setDossier(sousDossier);
            copie.setTypeDocument(source.getTypeDocument());
            copie.setDateRemise(source.getDateRemise());
            return copie;
        }).toList();
        remises.saveAll(copiesRemises);
    }
}
