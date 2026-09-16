package com.auximad.controller;

import com.auximad.dto.PageResponse;
import com.auximad.entity.*;
import com.auximad.security.AutorisationHelper;
import com.auximad.security.JwtSecurityContext;
import com.auximad.service.DossierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;import java.util.Map;
@RestController
@RequestMapping("/api/dossiers")
@Tag(name = "Dossiers", description = "API pour la gestion des dossiers de transit")
public class DossierController {

    private final DossierService dossierService;

    public DossierController(DossierService dossierService) {
        this.dossierService = dossierService;
    }

    @GetMapping
    @Operation(summary = "Rechercher les dossiers (FTS + filtres + filtre agence automatique)")
    public ResponseEntity<PageResponse<Dossier>> search(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer phaseId,
            @RequestParam(required = false) Integer statutId,
            @RequestParam(required = false) Integer clientId,
            @RequestParam(required = false) Integer typeProductionId,
            @RequestParam(required = false) Integer otId,
            @RequestParam(required = false) Boolean estGroupage,
            @RequestParam(required = false) String dateMin,
            @RequestParam(required = false) String dateMax,
            @RequestParam(required = false) String typeDossier  // "MERE", "SOUS_DOSSIER", ou null = tous
    ) {
        // Filtre automatique par agence si l'utilisateur n'a pas la portée TOUTES
        Integer agenceId = null;
        boolean peutVoirTout = JwtSecurityContext.aPermission(
                AutorisationHelper.PORTEE_TOUTES_LECTURE, AutorisationHelper.ACTION_OT);
        if (!peutVoirTout) {
            agenceId = JwtSecurityContext.getIdAgence();
        }

        return ResponseEntity.ok(dossierService.searchDossiers(
                page, size, sortBy, sortDir, query, phaseId, statutId,
                clientId, typeProductionId, otId, estGroupage, dateMin, dateMax,
                agenceId, typeDossier
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer un dossier par son ID")
    public ResponseEntity<Dossier> getById(@PathVariable Integer id) {
        return dossierService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Créer un nouveau dossier")
    public ResponseEntity<?> create(@RequestBody Dossier dossier) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(dossierService.saveDossier(dossier));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un dossier existant")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody Dossier dossier) {
        try {
            dossier.setId(id);
            return ResponseEntity.ok(dossierService.saveDossier(dossier));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un dossier")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        try {
            dossierService.deleteDossier(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            // Gestion des erreurs de validation métier
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Erreur lors de la suppression du dossier", 
                                 "details", e.getMessage()));
        } catch (Exception e) {
            // Gestion des erreurs de contrainte de base de données ou autres erreurs
            String errorMessage = "Erreur lors de la suppression du dossier";
            if (e.getMessage() != null && e.getMessage().contains("foreign key")) {
                errorMessage = "Impossible de supprimer ce dossier car il est lié à d'autres données dans le système";
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", errorMessage, 
                                 "details", e.getMessage() != null ? e.getMessage() : "Erreur interne"));
        }
    }

    // Référentiels
    @GetMapping("/phases")
    @Operation(summary = "Lister toutes les phases dossier")
    public ResponseEntity<List<PhaseDossier>> getPhases(
            @RequestParam(required = false) Integer modeTransportId) {
        if (modeTransportId != null) {
            return ResponseEntity.ok(dossierService.getPhasesByModeTransport(modeTransportId));
        }
        return ResponseEntity.ok(dossierService.getAllPhases());
    }

    @GetMapping("/statuts")
    @Operation(summary = "Lister tous les statuts de dossier")
    public ResponseEntity<List<StatutDossier>> getStatuts() {
        return ResponseEntity.ok(dossierService.getAllStatuts());
    }

    @GetMapping("/incontermes")
    @Operation(summary = "Lister tous les Incoterms")
    public ResponseEntity<List<Inconterme>> getIncontermes() {
        return ResponseEntity.ok(dossierService.getAllIncontermes());
    }

    // ─── Tableaux de bord Documents ──────────────────────────────────────────

    @GetMapping("/sans-documents")
    @Operation(summary = "Lister les dossiers avec des documents obligatoires manquants")
    public ResponseEntity<List<Dossier>> getDossiersSansDocuments() {
        Integer agenceId = null;
        boolean peutVoirTout = JwtSecurityContext.aPermission(
                AutorisationHelper.PORTEE_TOUTES_LECTURE, AutorisationHelper.ACTION_OT);
        if (!peutVoirTout) {
            agenceId = JwtSecurityContext.getIdAgence();
        }
        return ResponseEntity.ok(dossierService.getDossiersSansDocumentsComplets(agenceId));
    }

}
