package com.auximad.controller;

import com.auximad.entity.Document;
import com.auximad.entity.TypeDocument;
import com.auximad.service.DocumentService;
import com.auximad.service.DossierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "Documents", description = "API pour la gestion des documents des dossiers")
public class DocumentController {

    private final DocumentService documentService;
    private final DossierService dossierService;

    public DocumentController(DocumentService documentService, DossierService dossierService) {
        this.documentService = documentService;
        this.dossierService = dossierService;
    }

    // ─── Types de documents ───────────────────────────────────────────────────

    @GetMapping("/types")
    @Operation(summary = "Lister les types de documents (filtrable par type de production)")
    public ResponseEntity<List<TypeDocument>> getTypeDocuments(
            @RequestParam(required = false) Integer typeProductionId) {
        return ResponseEntity.ok(documentService.getTypeDocumentsByTypeProduction(typeProductionId));
    }

    // ─── Documents d'un dossier ───────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Lister les documents actifs d'un dossier")
    public ResponseEntity<List<Document>> getDocuments(@RequestParam Integer dossierId) {
        return ResponseEntity.ok(documentService.getDocumentsByDossier(dossierId));
    }

    @GetMapping("/verification/{dossierId}")
    @Operation(summary = "Vérifier si tous les documents obligatoires sont remis")
    public ResponseEntity<Map<String, Boolean>> verifierDocuments(@PathVariable Integer dossierId) {
        boolean complet = documentService.tousDocsObligatoiresRemis(dossierId);
        return ResponseEntity.ok(Map.of("complet", complet));
    }

    // ─── Ajouter un document ──────────────────────────────────────────────────

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Ajouter/mettre à jour un document pour un dossier (avec fichier optionnel)")
    public ResponseEntity<?> addDocument(
            @RequestParam Integer dossierId,
            @RequestParam Integer typeDocumentId,
            @RequestParam(required = false) MultipartFile fichier) {
        try {
            Document doc = documentService.saveDocument(dossierId, typeDocumentId, fichier);
            return ResponseEntity.ok(doc);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Erreur lors de l'upload : " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ─── Traitement batch ─────────────────────────────────────────────────────

    @PostMapping(value = "/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Traitement batch : cocher/décocher des documents pour un dossier")
    public ResponseEntity<?> processerBatch(
            @RequestParam Integer dossierId,
            @RequestParam(required = false) List<Integer> typeIdsAjoutes,
            @RequestParam(required = false) List<Integer> typeIdsRetires,
            @RequestParam(required = false) List<MultipartFile> fichiers,
            @RequestParam(required = false) List<Integer> typeIdsFichiers) {
        try {
            documentService.processerBatchDocuments(dossierId, typeIdsAjoutes, typeIdsRetires, fichiers, typeIdsFichiers);
            return ResponseEntity.ok(Map.of("message", "Documents mis à jour avec succès"));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Erreur lors de l'upload : " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ─── Retirer un document ──────────────────────────────────────────────────

    @DeleteMapping("/retirer")
    @Operation(summary = "Retirer (désactiver) un document d'un dossier")
    public ResponseEntity<?> retirerDocument(
            @RequestParam Integer dossierId,
            @RequestParam Integer typeDocumentId) {
        documentService.retirerDocument(dossierId, typeDocumentId);
        return ResponseEntity.ok(Map.of("message", "Document retiré"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer définitivement un document")
    public ResponseEntity<Void> deleteDocument(@PathVariable Integer id) {
        documentService.deleteDocument(id);
        return ResponseEntity.ok().build();
    }

    // ─── Visualiser un fichier ────────────────────────────────────────────────

    @GetMapping("/{id}/fichier")
    @Operation(summary = "Télécharger/visualiser le fichier d'un document")
    public ResponseEntity<Resource> getFichier(@PathVariable Integer id) {
        Document doc = documentService.getById(id).orElse(null);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }

        if (doc.getCheminFichier() == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            Path fichierPath = documentService.getFichierPath(doc.getCheminFichier());
            Resource resource = new UrlResource(fichierPath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = determinerContentType(doc.getNomFichier());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + doc.getNomFichier() + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private String determinerContentType(String nomFichier) {
        if (nomFichier == null) return "application/octet-stream";
        String lower = nomFichier.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".doc")) return "application/msword";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        return "application/octet-stream";
    }
}
