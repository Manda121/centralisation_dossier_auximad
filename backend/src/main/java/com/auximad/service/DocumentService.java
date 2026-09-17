package com.auximad.service;

import com.auximad.entity.*;
import com.auximad.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final TypeDocumentRepository typeDocumentRepository;
    private final DossierRepository dossierRepository;
    private final RemiseRepository remiseRepository;

    @Value("${app.upload.dir:uploads/documents}")
    private String uploadDir;

    public DocumentService(DocumentRepository documentRepository,
                           TypeDocumentRepository typeDocumentRepository,
                           DossierRepository dossierRepository,
                           RemiseRepository remiseRepository) {
        this.documentRepository = documentRepository;
        this.typeDocumentRepository = typeDocumentRepository;
        this.dossierRepository = dossierRepository;
        this.remiseRepository = remiseRepository;
    }

    // ─── Remises (table remise) ─────────────────────────────────────────────

    /** Liste les remises d'un dossier */
    @Transactional(readOnly = true)
    public List<Remise> getRemisesByDossier(Integer dossierId) {
        return remiseRepository.findByDossierId(dossierId);
    }

    /** Remises urgentes : dont la date de remise tombe dans les N prochains jours */
    @Transactional(readOnly = true)
    public List<Remise> getRemisesUrgentes(int jours, Integer agenceId) {
        LocalDateTime maintenant = LocalDateTime.now();
        LocalDateTime limite = maintenant.plusDays(jours);
        if (agenceId != null) {
            return remiseRepository.findRemisesUrgentesParAgence(maintenant, limite, agenceId);
        }
        return remiseRepository.findRemisesUrgentes(maintenant, limite);
    }

    /**
     * Assigne (remet) des types de documents à un dossier avec une date de remise
     * enregistrée dans la table remise (pas sur le dossier).
     */
    @Transactional
    public List<Remise> assignerDocuments(Integer dossierId,
                                          List<Integer> typeDocumentIds,
                                          LocalDateTime dateRemise) {
        Dossier dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new IllegalArgumentException("Dossier introuvable : " + dossierId));
        if (typeDocumentIds == null || typeDocumentIds.isEmpty()) {
            throw new IllegalArgumentException("Aucun document à assigner.");
        }

        List<Remise> resultats = new ArrayList<>();
        for (Integer typeId : typeDocumentIds) {
            resultats.add(assignerDocument(dossier, typeId, dateRemise));
        }
        return resultats;
    }

    /** Assigne un type de document à un dossier avec sa date de remise */
    @Transactional
    public Remise assignerDocument(Integer dossierId, Integer typeDocumentId, LocalDateTime dateRemise) {
        Dossier dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new IllegalArgumentException("Dossier introuvable : " + dossierId));
        return assignerDocument(dossier, typeDocumentId, dateRemise);
    }

    private Remise assignerDocument(Dossier dossier, Integer typeDocumentId, LocalDateTime dateRemise) {
        TypeDocument typeDocument = typeDocumentRepository.findById(typeDocumentId)
                .orElseThrow(() -> new IllegalArgumentException("Type de document introuvable : " + typeDocumentId));

        // Créer / mettre à jour la remise dans la table remise
        // (ne pas écraser une date de remise existante si aucune nouvelle date fournie)
        Remise remise = remiseRepository.findByDossierIdAndTypeDocumentId(dossier.getId(), typeDocumentId)
                .orElseGet(Remise::new);
        remise.setDossier(dossier);
        remise.setTypeDocument(typeDocument);
        if (remise.getDateRemise() == null || dateRemise != null) {
            remise.setDateRemise(dateRemise != null ? dateRemise : LocalDateTime.now());
        }
        Remise saved = remiseRepository.save(remise);

        // Créer aussi le document associé si absent (référentiel des docs remis)
        if (!documentRepository.existsByDossierIdAndTypeDocumentIdAndEstActifTrue(dossier.getId(), typeDocumentId)) {
            Document doc = new Document();
            doc.setDossier(dossier);
            doc.setTypeDocument(typeDocument);
            doc.setEstActif(true);
            doc.setDateAjout(LocalDateTime.now());
            documentRepository.save(doc);
        }
        return saved;
    }

    /** Liste les types de documents pour un type de production donné */
    @Transactional(readOnly = true)
    public List<TypeDocument> getTypeDocumentsByTypeProduction(Integer typeProductionId) {
        if (typeProductionId == null) {
            return typeDocumentRepository.findAll();
        }
        return typeDocumentRepository.findByTypeProductionId(typeProductionId);
    }

    /** Liste tous les types de documents */
    @Transactional(readOnly = true)
    public List<TypeDocument> getAllTypeDocuments() {
        return typeDocumentRepository.findAll();
    }

    /** Documents actifs d'un dossier */
    @Transactional(readOnly = true)
    public List<Document> getDocumentsByDossier(Integer dossierId) {
        return documentRepository.findByDossierIdAndEstActifTrue(dossierId);
    }

    /** Vérifie si tous les documents obligatoires sont remis pour un dossier */
    @Transactional(readOnly = true)
    public boolean tousDocsObligatoiresRemis(Integer dossierId) {
        Dossier dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new IllegalArgumentException("Dossier introuvable : " + dossierId));
        if (dossier.getTypeProduction() == null) return true;
        long manquants = documentRepository.countDocsObligatoiresManquants(
                dossierId, dossier.getTypeProduction().getId());
        return manquants == 0;
    }

    /**
     * Sauvegarde un document (ajout ou mise à jour) avec fichier optionnel
     */
    @Transactional
    public Document saveDocument(Integer dossierId, Integer typeDocumentId, MultipartFile fichier) throws IOException {
        Dossier dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new IllegalArgumentException("Dossier introuvable : " + dossierId));
        TypeDocument typeDocument = typeDocumentRepository.findById(typeDocumentId)
                .orElseThrow(() -> new IllegalArgumentException("Type de document introuvable : " + typeDocumentId));

        // Chercher un document existant actif du même type pour ce dossier
        Optional<Document> existant = documentRepository.findByDossierIdAndTypeDocumentIdAndEstActifTrue(dossierId, typeDocumentId);
        Document doc = existant.orElseGet(Document::new);

        doc.setDossier(dossier);
        doc.setTypeDocument(typeDocument);
        doc.setEstActif(true);
        doc.setDateAjout(LocalDateTime.now());

        // Upload fichier si présent
        if (fichier != null && !fichier.isEmpty()) {
            String cheminFichier = sauvegarderFichier(fichier, dossierId);
            doc.setCheminFichier(cheminFichier);
            doc.setNomFichier(fichier.getOriginalFilename());
        }

        Document saved = documentRepository.save(doc);

        // Garantir qu'une remise existe dès qu'un document est actif
        // (sans écraser la date de remise déjà enregistrée)
        remiseRepository.findByDossierIdAndTypeDocumentId(dossierId, typeDocumentId)
                .orElseGet(() -> {
                    Remise r = new Remise();
                    r.setDossier(dossier);
                    r.setTypeDocument(typeDocument);
                    r.setDateRemise(LocalDateTime.now());
                    return remiseRepository.save(r);
                });

        return saved;
    }

    /**
     * Retire (désactive) un document d'un dossier et supprime sa remise associée
     */
    @Transactional
    public void retirerDocument(Integer dossierId, Integer typeDocumentId) {
        Optional<Document> doc = documentRepository.findByDossierIdAndTypeDocumentIdAndEstActifTrue(dossierId, typeDocumentId);
        doc.ifPresent(d -> {
            d.setEstActif(false);
            documentRepository.save(d);
        });
        remiseRepository.findByDossierIdAndTypeDocumentId(dossierId, typeDocumentId)
                .ifPresent(remiseRepository::delete);
    }

    /**
     * Met à jour uniquement la date de remise d'un document d'un dossier
     */
    @Transactional
    public Remise mettreAJourDateRemise(Integer dossierId, Integer typeDocumentId, LocalDateTime dateRemise) {
        Remise remise = remiseRepository.findByDossierIdAndTypeDocumentId(dossierId, typeDocumentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Aucune remise trouvée pour ce document. Assignez d'abord le document."));
        remise.setDateRemise(dateRemise);
        return remiseRepository.save(remise);
    }

    /** Map des remises d'un dossier indexées par id de type de document */
    @Transactional(readOnly = true)
    public Map<Integer, LocalDateTime> getDatesRemiseParType(Integer dossierId) {
        Map<Integer, LocalDateTime> dates = new java.util.HashMap<>();
        for (Remise r : remiseRepository.findByDossierId(dossierId)) {
            dates.put(r.getTypeDocument().getId(), r.getDateRemise());
        }
        return dates;
    }

    /**
     * Traitement batch : cocher/décocher plusieurs types de documents pour un dossier.
     * Vérifie si le dossier doit être fermé (dépassement date limite).
     */
    @Transactional
    public void processerBatchDocuments(Integer dossierId,
                                        List<Integer> typeIdsAjoutes,
                                        List<Integer> typeIdsRetires,
                                        List<MultipartFile> fichiers,
                                        List<Integer> typeIdsFichiers) throws IOException {
        // Retirer les documents décochés
        if (typeIdsRetires != null) {
            for (Integer typeId : typeIdsRetires) {
                retirerDocument(dossierId, typeId);
            }
        }

        // Ajouter les documents nouvellement cochés
        if (typeIdsAjoutes != null) {
            for (Integer typeId : typeIdsAjoutes) {
                boolean aFichier = (typeIdsFichiers != null && typeIdsFichiers.contains(typeId));
                if (!aFichier) {
                    saveDocument(dossierId, typeId, null);
                }
            }
        }

        // Traiter tous les fichiers téléchargés (pour les docs nouvellement cochés OU déjà cochés)
        if (typeIdsFichiers != null && fichiers != null) {
            for (int i = 0; i < typeIdsFichiers.size(); i++) {
                Integer typeId = typeIdsFichiers.get(i);
                if (i < fichiers.size()) {
                    MultipartFile fichier = fichiers.get(i);
                    if (fichier != null && !fichier.isEmpty()) {
                        saveDocument(dossierId, typeId, fichier);
                    }
                }
            }
        }
    }

    /**
     * Sauvegarder physiquement le fichier sur le serveur
     */
    private String sauvegarderFichier(MultipartFile fichier, Integer dossierId) throws IOException {
        Path repertoire = Paths.get(uploadDir, "dossier-" + dossierId).toAbsolutePath();
        Files.createDirectories(repertoire);
        String nomFichier = UUID.randomUUID() + "_" + fichier.getOriginalFilename();
        Path destination = repertoire.resolve(nomFichier).toAbsolutePath();
        fichier.transferTo(destination.toFile());
        return "dossier-" + dossierId + "/" + nomFichier;
    }

    /**
     * Chemin physique pour servir un fichier
     */
    public Path getFichierPath(String cheminRelatif) {
        return Paths.get(uploadDir, cheminRelatif);
    }

    /** Récupère un document par ID */
    @Transactional(readOnly = true)
    public Optional<Document> getById(Integer id) {
        return documentRepository.findById(id);
    }

    /** Supprime définitivement un document */
    @Transactional
    public void deleteDocument(Integer id) {
        documentRepository.deleteById(id);
    }
}
