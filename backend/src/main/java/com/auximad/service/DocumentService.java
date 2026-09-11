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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final TypeDocumentRepository typeDocumentRepository;
    private final DossierRepository dossierRepository;

    @Value("${app.upload.dir:uploads/documents}")
    private String uploadDir;

    public DocumentService(DocumentRepository documentRepository,
                           TypeDocumentRepository typeDocumentRepository,
                           DossierRepository dossierRepository) {
        this.documentRepository = documentRepository;
        this.typeDocumentRepository = typeDocumentRepository;
        this.dossierRepository = dossierRepository;
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

        return documentRepository.save(doc);
    }

    /**
     * Retire (désactive) un document d'un dossier
     */
    @Transactional
    public void retirerDocument(Integer dossierId, Integer typeDocumentId) {
        Optional<Document> doc = documentRepository.findByDossierIdAndTypeDocumentIdAndEstActifTrue(dossierId, typeDocumentId);
        doc.ifPresent(d -> {
            d.setEstActif(false);
            documentRepository.save(d);
        });
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

        // Ajouter les documents cochés
        if (typeIdsAjoutes != null) {
            for (int i = 0; i < typeIdsAjoutes.size(); i++) {
                Integer typeId = typeIdsAjoutes.get(i);
                MultipartFile fichier = null;
                if (fichiers != null && typeIdsFichiers != null) {
                    int idx = typeIdsFichiers.indexOf(typeId);
                    if (idx >= 0 && idx < fichiers.size()) {
                        fichier = fichiers.get(idx);
                    }
                }
                saveDocument(dossierId, typeId, fichier);
            }
        }

        // Vérifier dépassement date limite → fermer dossier si nécessaire
        verifierEtFermerParDateLimite(dossierId);
    }

    /**
     * Ferme automatiquement le dossier si la date limite est dépassée
     * et que des documents obligatoires viennent d'être remis après cette date
     */
    @Transactional
    public void verifierEtFermerParDateLimite(Integer dossierId) {
        Dossier dossier = dossierRepository.findById(dossierId).orElse(null);
        if (dossier == null || dossier.getDateLimiteRemise() == null) return;
        if (LocalDateTime.now().isAfter(dossier.getDateLimiteRemise())) {
            // Marquer le dossier comme "fermé" (statut id=3 par convention, à adapter)
            // On marque via un flag @Transient ou on laisse le frontend afficher l'alerte
            // Pour l'instant on persiste un statut "FERME" si disponible
            dossierRepository.findById(dossierId).ifPresent(d -> {
                // Le statut "fermé" devra être configuré côté référentiel
                // Pour l'instant, on ne touche pas le statut automatiquement
                // (laissé à l'utilisateur via alerte)
            });
        }
    }

    /**
     * Sauvegarder physiquement le fichier sur le serveur
     */
    private String sauvegarderFichier(MultipartFile fichier, Integer dossierId) throws IOException {
        Path repertoire = Paths.get(uploadDir, "dossier-" + dossierId);
        Files.createDirectories(repertoire);
        String nomFichier = UUID.randomUUID() + "_" + fichier.getOriginalFilename();
        Path destination = repertoire.resolve(nomFichier);
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
