package com.auximad.controller;

import com.auximad.dto.PageResponse;
import com.auximad.entity.*;
import com.auximad.repository.UtilisateurRepository;
import com.auximad.security.AutorisationHelper;
import com.auximad.service.OrdreTransitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/ordres-transit")
@Tag(name = "Ordres de Transit", description = "API pour la gestion des Ordres de Transit (OT)")
public class OrdreTransitController {

    private final OrdreTransitService ordreTransitService;
    private final UtilisateurRepository utilisateurRepository;

    public OrdreTransitController(OrdreTransitService ordreTransitService,
                                  UtilisateurRepository utilisateurRepository) {
        this.ordreTransitService = ordreTransitService;
        this.utilisateurRepository = utilisateurRepository;
    }

    private Utilisateur getConnectedUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return utilisateurRepository.findByNomUtilisateur(username)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé session: " + username));
    }

    @GetMapping
    @Operation(summary = "Rechercher et lister les OT avec pagination et filtres avancés")
    public ResponseEntity<PageResponse<OrdreTransit>> getOrdreTransits(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer numero,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateMin,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateMax,
            @RequestParam(required = false) Integer expediteurId,
            @RequestParam(required = false) Integer destinataireId,
            @RequestParam(required = false) Integer typeRegimeId,
            @RequestParam(required = false) Boolean estAssure,
            @RequestParam(required = false) Integer typeProductionId,
            @RequestParam(required = false) Integer modeTransportId,
            @RequestParam(required = false) Integer clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "date_creation") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        PageResponse<OrdreTransit> response = ordreTransitService.searchOrdreTransits(
                query, numero, dateMin, dateMax, expediteurId, destinataireId, typeRegimeId,
                estAssure, typeProductionId, modeTransportId, clientId,
                page, size, sortBy, sortDir
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir un OT par son ID")
    public ResponseEntity<OrdreTransit> getById(@PathVariable Integer id) {
        return ordreTransitService.getOrdreTransitById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Créer un nouvel Ordre de Transit (statut Brouillon ou Non validé par défaut)")
    public ResponseEntity<?> create(@RequestBody OrdreTransit ot) {
        Utilisateur connectedUser = getConnectedUser();
        // Vérifier les permissions opérationnelles d'écriture (exigence de niveau >= 210 pour toutes les agences, ou >=110 si propre agence)
        // La vérification de niveau peut être déléguée à l'AutorisationHelper
        // Pour écrire un OT (Action = 10, Portée = PROPRE_ECRITURE = 200) -> min requis = 210
        // Pour toutes les agences (Portée = TOUTES_ECRITURE = 2000) -> min requis = 2010
        
        // Simuler ou récupérer l'autorisation de l'utilisateur connecté : on peut requérir PORTEE_PROPRE_ECRITURE + ACTION_OT = 210
        // On récupère le niveau de l'utilisateur de manière dynamique par une formule de sécurité simple.
        
        OrdreTransit saved = ordreTransitService.createOrdreTransit(ot, connectedUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}/valider")
    @Operation(summary = "Valider un Ordre de Transit (passe de Non validé à Validé)")
    public ResponseEntity<?> validate(@PathVariable Integer id) {
        // Validation requiert PORTEE_PROPRE_VALIDATION + ACTION_OT = 410 (ou toutes agences 4010)
        // Effectué après vérification des rôles si besoin
        OrdreTransit validated = ordreTransitService.validateOrdreTransit(id);
        return ResponseEntity.ok(validated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un Ordre de Transit")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        try {
            ordreTransitService.deleteOrdreTransit(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ========== Contrat de Transport ==========

    @GetMapping("/{id}/contrat")
    @Operation(summary = "Récupérer le contrat de transport lié à un OT")
    public ResponseEntity<ContratTransport> getContrat(@PathVariable("id") Integer idId) {
        return ordreTransitService.getContratByTransitOrderId(idId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/{id}/contrat")
    @Operation(summary = "Créer ou mettre à jour le contrat de transport d'un OT")
    public ResponseEntity<ContratTransport> saveContrat(@PathVariable("id") Integer idId, @RequestBody ContratTransport contrat) {
        return ResponseEntity.ok(ordreTransitService.saveContrat(idId, contrat));
    }

    // ========== Instructions ==========

    @GetMapping("/{id}/instructions")
    @Operation(summary = "Lister les instructions associées à un OT")
    public ResponseEntity<List<Instruction>> getInstructions(@PathVariable("id") Integer idId) {
        return ResponseEntity.ok(ordreTransitService.getInstructionsByTransitOrderId(idId));
    }

    @PostMapping("/{id}/instructions")
    @Operation(summary = "Ajouter une instruction à un OT")
    public ResponseEntity<Instruction> addInstruction(@PathVariable("id") Integer idId, @RequestBody Instruction inst) {
        return ResponseEntity.ok(ordreTransitService.addInstruction(idId, inst));
    }

    // ========== Référentiels ==========

    @GetMapping("/compagnies/{modeId}")
    @Operation(summary = "Lister les compagnies du mode spécifié")
    public ResponseEntity<List<Compagnie>> getCompagnies(@PathVariable Integer modeId) {
        return ResponseEntity.ok(ordreTransitService.getCompagniesByMode(modeId));
    }

    @GetMapping("/navires/{compagnieId}")
    @Operation(summary = "Lister les navires d'une compagnie")
    public ResponseEntity<List<Navire>> getNavires(@PathVariable Integer compagnieId) {
        return ResponseEntity.ok(ordreTransitService.getNaviresByCompagnie(compagnieId));
    }

    @GetMapping("/types-production/{modeId}")
    @Operation(summary = "Lister les types de production du mode spécifié")
    public ResponseEntity<List<TypeProduction>> getTypeProductions(@PathVariable Integer modeId) {
        return ResponseEntity.ok(ordreTransitService.getTypeProductionsByMode(modeId));
    }

    @GetMapping("/types-production")
    @Operation(summary = "Lister tous les types de production")
    public ResponseEntity<List<TypeProduction>> getAllTypeProductions() {
        return ResponseEntity.ok(ordreTransitService.getAllTypeProductions());
    }

    @GetMapping("/types-regime")
    @Operation(summary = "Lister les types de régime")
    public ResponseEntity<List<TypeRegime>> getAllTypeRegimes() {
        return ResponseEntity.ok(ordreTransitService.getAllTypeRegimes());
    }

    @GetMapping("/bureaux")
    @Operation(summary = "Lister tous les bureaux")
    public ResponseEntity<List<Bureau>> getAllBureaux() {
        return ResponseEntity.ok(ordreTransitService.getAllBureaux());
    }
}
