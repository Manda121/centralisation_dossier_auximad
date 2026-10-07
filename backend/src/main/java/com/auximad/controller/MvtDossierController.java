package com.auximad.controller;

import com.auximad.dto.MvtDossierRequest;
import com.auximad.dto.OperationDossierStatusDTO;
import com.auximad.dto.PageResponse;
import com.auximad.entity.MvtDossier;
import com.auximad.service.MvtDossierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mouvements-dossier")
@Tag(name = "Mouvements Dossier", description = "API pour le suivi et l'enregistrement des mouvements de dossier")
public class MvtDossierController {

    private final MvtDossierService mvtDossierService;

    public MvtDossierController(MvtDossierService mvtDossierService) {
        this.mvtDossierService = mvtDossierService;
    }

    @GetMapping
    @Operation(summary = "Rechercher des mouvements de dossier (filtrage multi-critères + pagination backend)")
    public ResponseEntity<PageResponse<MvtDossier>> search(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer dossierId,
            @RequestParam(required = false) String numeroDossier,
            @RequestParam(required = false) String cpClient,
            @RequestParam(required = false) Integer agenceId,
            @RequestParam(required = false) Integer typeProductionId,
            @RequestParam(required = false) Integer phaseId,
            @RequestParam(required = false) Integer statutId,
            @RequestParam(required = false) Integer operationId,
            @RequestParam(required = false) Integer utilisateurId,
            @RequestParam(required = false) String dateMin,
            @RequestParam(required = false) String dateMax
    ) {
        return ResponseEntity.ok(mvtDossierService.searchMouvements(
                page, size, sortBy, sortDir, query,
                dossierId, numeroDossier, cpClient, agenceId, typeProductionId,
                phaseId, statutId, operationId, utilisateurId, dateMin, dateMax
        ));
    }

    @GetMapping("/dossier/{dossierId}/operations-status")
    @Operation(summary = "Obtenir la liste des opérations et leur statut pour un dossier")
    public ResponseEntity<List<OperationDossierStatusDTO>> getOperationsStatus(@PathVariable Integer dossierId) {
        return ResponseEntity.ok(mvtDossierService.getOperationsStatusForDossier(dossierId));
    }

    @PostMapping
    @Operation(summary = "Enregistrer un nouveau mouvement de dossier avec fichier optionnel")
    public ResponseEntity<?> createMouvement(
            @RequestParam("dossierId") Integer dossierId,
            @RequestParam("operationId") Integer operationId,
            @RequestParam(value = "dateMvt", required = false) String dateMvtStr,
            @RequestParam(value = "commentaire", required = false) String commentaire,
            @RequestPart(value = "fichier", required = false) MultipartFile file
    ) {
        try {
            MvtDossierRequest req = new MvtDossierRequest();
            req.setDossierId(dossierId);
            req.setOperationId(operationId);
            req.setCommentaire(commentaire);
            if (dateMvtStr != null && !dateMvtStr.isBlank()) {
                req.setDateMvt(LocalDateTime.parse(dateMvtStr));
            } else {
                req.setDateMvt(LocalDateTime.now());
            }

            MvtDossier saved = mvtDossierService.creerMouvement(req, file);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("erreur", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erreur", "Erreur lors de la création du mouvement", "details", e.getMessage() != null ? e.getMessage() : ""));
        }
    }
}
