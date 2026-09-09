package com.auximad.controller;

import com.auximad.dto.DemandeInteragenceReponseRequest;
import com.auximad.dto.DemandeInteragenceRequest;
import com.auximad.dto.PageResponse;
import com.auximad.entity.DemandeInteragence;
import com.auximad.service.DemandeInteragenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/demandes-interagence")
@Tag(name = "Demandes Inter-Agences", description = "API pour la demande et validation des partages inter-agences")
public class DemandeInteragenceController {

    private final DemandeInteragenceService demandeService;

    public DemandeInteragenceController(DemandeInteragenceService demandeService) {
        this.demandeService = demandeService;
    }

    @PostMapping
    @Operation(summary = "Créer une demande d'inter-agence pour un dossier")
    public ResponseEntity<?> creer(@RequestBody DemandeInteragenceRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(demandeService.creerDemande(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping
    @Operation(summary = "Rechercher et filtrer les demandes d'inter-agence")
    public ResponseEntity<PageResponse<DemandeInteragence>> search(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(required = false) String dateMin,
            @RequestParam(required = false) String dateMax,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) Integer agenceDemandeuseId,
            @RequestParam(required = false) Integer agenceDestinataireId,
            @RequestParam(required = false) String query
    ) {
        return ResponseEntity.ok(demandeService.searchDemandes(
                page, size, sortBy, sortDir, dateMin, dateMax, statut,
                agenceDemandeuseId, agenceDestinataireId, query
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir les détails d'une demande par son ID")
    public ResponseEntity<DemandeInteragence> getById(@PathVariable Integer id) {
        return demandeService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/repondre")
    @Operation(summary = "Accepter ou refuser une demande d'inter-agence")
    public ResponseEntity<?> repondre(@PathVariable Integer id, @RequestBody DemandeInteragenceReponseRequest reponse) {
        try {
            return ResponseEntity.ok(demandeService.repondreDemande(id, reponse));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une demande d'inter-agence en attente (motif ou agence destinataire)")
    public ResponseEntity<?> modifier(@PathVariable Integer id, @RequestBody DemandeInteragenceRequest request) {
        try {
            return ResponseEntity.ok(demandeService.modifierDemande(id, request));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/non-vues-count")
    @Operation(summary = "Compter les demandes en attente non vues pour l'agence de l'utilisateur")
    public ResponseEntity<Map<String, Long>> contarNonVues() {
        return ResponseEntity.ok(Map.of("count", demandeService.compterNonVues()));
    }

    @GetMapping("/dossier/{dossierId}")
    @Operation(summary = "Obtenir toutes les demandes d'inter-agence liées à un dossier")
    public ResponseEntity<List<DemandeInteragence>> getParDossier(@PathVariable Integer dossierId) {
        return ResponseEntity.ok(demandeService.getDemandesParDossier(dossierId));
    }
}
