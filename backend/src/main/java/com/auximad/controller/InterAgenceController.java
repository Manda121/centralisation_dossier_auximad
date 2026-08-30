package com.auximad.controller;

import com.auximad.dto.SousDossierRequest;
import com.auximad.entity.DossierInteragence;
import com.auximad.service.InterAgenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inter-agences")
@Tag(name = "Inter-Agences", description = "API pour la gestion des sous-dossiers inter-agences")
public class InterAgenceController {

    private final InterAgenceService interAgenceService;

    public InterAgenceController(InterAgenceService interAgenceService) {
        this.interAgenceService = interAgenceService;
    }

    /**
     * Crée un sous-dossier lié à un dossier mère.
     * Requiert niveau >= 220 (PROPRE_ECRITURE + ACTION_SOUS_DOSSIER).
     */
    @PostMapping("/sous-dossiers/{idDossierMere}")
    @Operation(summary = "Créer un sous-dossier inter-agences à partir d'un dossier mère")
    public ResponseEntity<?> creerSousDossier(
            @PathVariable Integer idDossierMere,
            @RequestBody SousDossierRequest request) {
        try {
            DossierInteragence resultat = interAgenceService.creerSousDossier(idDossierMere, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(resultat);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Liste tous les sous-dossiers d'un dossier mère.
     */
    @GetMapping("/sous-dossiers/{idDossierMere}")
    @Operation(summary = "Lister les sous-dossiers d'un dossier mère")
    public ResponseEntity<List<DossierInteragence>> getSousDossiers(
            @PathVariable Integer idDossierMere) {
        return ResponseEntity.ok(interAgenceService.getSousDossiers(idDossierMere));
    }

    /**
     * Retourne le lien interagence d'un sous-dossier (pour retrouver son dossier mère).
     */
    @GetMapping("/dossier-mere/{idSousDossier}")
    @Operation(summary = "Obtenir le dossier mère d'un sous-dossier")
    public ResponseEntity<DossierInteragence> getDossierMere(
            @PathVariable Integer idSousDossier) {
        return interAgenceService.getDossierMere(idSousDossier)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
