package com.auximad.controller;

import com.auximad.dto.PageResponse;
import com.auximad.entity.Action;
import com.auximad.entity.Historique;
import com.auximad.repository.ActionRepository;
import com.auximad.repository.HistoriqueRepository;
import com.auximad.repository.UtilisateurRepository;
import com.auximad.security.AutorisationHelper;
import com.auximad.security.JwtSecurityContext;
import com.auximad.service.HistoriqueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/historiques")
@Tag(name = "Historiques", description = "API de consultation de l'historique des actions (audit)")
public class HistoriqueController {

    private final HistoriqueService historiqueService;
    private final ActionRepository actionRepository;
    private final HistoriqueRepository historiqueRepository;
    private final UtilisateurRepository utilisateurRepository;

    public HistoriqueController(HistoriqueService historiqueService,
                                ActionRepository actionRepository,
                                HistoriqueRepository historiqueRepository,
                                UtilisateurRepository utilisateurRepository) {
        this.historiqueService = historiqueService;
        this.actionRepository = actionRepository;
        this.historiqueRepository = historiqueRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    @GetMapping
    @Operation(summary = "Recherche multi-critères paginée de l'historique")
    public ResponseEntity<PageResponse<Historique>> search(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer utilisateurId,
            @RequestParam(required = false) Integer actionId,
            @RequestParam(required = false) String adresseIp,
            @RequestParam(required = false) String hostname,
            @RequestParam(required = false) String logiciel,
            @RequestParam(required = false) String methodeHttp,
            @RequestParam(required = false) Integer statutHttp,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateMin,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateMax
    ) {
        // Restriction de portée : seuls les utilisateurs avec LECTURE toutes agences
        // voient tout l'historique ; les autres voient uniquement leurs propres actions.
        Integer idUtilisateurFiltre = utilisateurId;
        boolean peutVoirTout = JwtSecurityContext.aPermission(
                AutorisationHelper.PORTEE_TOUTES_LECTURE, AutorisationHelper.ACTION_OT);
        if (!peutVoirTout) {
            idUtilisateurFiltre = JwtSecurityContext.getIdUtilisateur();
        }

        PageResponse<Historique> result = historiqueService.searchHistoriques(
                page, size, sortBy, sortDir, query, idUtilisateurFiltre, actionId,
                adresseIp, hostname, logiciel, methodeHttp, statutHttp, dateMin, dateMax);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/actions")
    @Operation(summary = "Lister les types d'actions (pour les filtres)")
    public ResponseEntity<List<Action>> getActions() {
        return ResponseEntity.ok(actionRepository.findAll());
    }

    @GetMapping("/utilisateurs")
    @Operation(summary = "Lister les utilisateurs (id + nom affichable) pour les filtres")
    public ResponseEntity<List<UtilisateurLite>> getUtilisateurs() {
        List<UtilisateurLite> utilisateurs = utilisateurRepository.findAll().stream()
                .map(u -> new UtilisateurLite(u.getId(), u.getNomUtilisateur(), u.getNomComplet()))
                .toList();
        return ResponseEntity.ok(utilisateurs);
    }

    @GetMapping("/filtres")
    @Operation(summary = "Valeurs distinctes pour les filtres (IP, hostname, logiciel)")
    public ResponseEntity<FiltresResponse> getFiltres() {
        FiltresResponse response = new FiltresResponse(
                historiqueRepository.findDistinctAdresseIps(),
                historiqueRepository.findDistinctHostnames(),
                historiqueRepository.findDistinctLogiciels()
        );
        return ResponseEntity.ok(response);
    }

    /** DTO simple pour les listes de valeurs distinctes. */
    public static class FiltresResponse {
        public List<String> adressesIp;
        public List<String> hostnames;
        public List<String> logiciels;

        public FiltresResponse(List<String> adressesIp, List<String> hostnames, List<String> logiciels) {
            this.adressesIp = adressesIp;
            this.hostnames = hostnames;
            this.logiciels = logiciels;
        }
    }

    /** DTO léger utilisateur pour le filtre. */
    public record UtilisateurLite(Integer id, String nomUtilisateur, String nomComplet) {
    }
}
