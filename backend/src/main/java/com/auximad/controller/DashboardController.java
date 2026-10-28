package com.auximad.controller;

import com.auximad.security.AutorisationHelper;
import com.auximad.security.JwtSecurityContext;
import com.auximad.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Vue dossiers du tableau de bord : KPI, répartition par phase, évolution des créations,
     * répartition par agence, dossiers prioritaires et activités récentes.
     * Le périmètre agence est imposé côté serveur pour les utilisateurs sans portée toutes-lecture.
     */
    @GetMapping("/dossiers")
    public ResponseEntity<Map<String, Object>> getVueDossiers(
            @RequestParam(required = false) String dateDebut,
            @RequestParam(required = false) String dateFin,
            @RequestParam(required = false) Integer agenceId,
            @RequestParam(required = false) Integer typeProductionId) {
        boolean peutVoirToutesAgences = JwtSecurityContext.aPermission(
                AutorisationHelper.PORTEE_TOUTES_LECTURE, AutorisationHelper.ACTION_OT);
        Integer agenceEffective = peutVoirToutesAgences ? agenceId : JwtSecurityContext.getIdAgence();
        return ResponseEntity.ok(dashboardService.getVueDossiers(agenceEffective, typeProductionId, dateDebut, dateFin));
    }

    /** Statistiques globales : compteurs de toutes les tables principales */
    @GetMapping("/global")
    public ResponseEntity<Map<String, Object>> getGlobal() {
        return ResponseEntity.ok(dashboardService.getStatsGlobales());
    }

    /** Statistiques spécifiques à un module */
    @GetMapping("/module/{module}")
    public ResponseEntity<Map<String, Object>> getModuleStats(@PathVariable String module) {
        Map<String, Object> result = switch (module.toLowerCase()) {
            case "dossiers"   -> dashboardService.getStatsDossiers();
            case "ot"         -> dashboardService.getStatsOT();
            case "clients"    -> dashboardService.getStatsClients();
            case "mouvements" -> dashboardService.getStatsMouvements();
            case "conteneurs" -> dashboardService.getStatsConteneurs();
            case "documents"  -> dashboardService.getStatsDocuments();
            default -> Map.of("error", "Module inconnu : " + module);
        };
        return ResponseEntity.ok(result);
    }

    /** Activité récente : 10 derniers mouvements de dossier */
    @GetMapping("/activite")
    public ResponseEntity<List<Map<String, Object>>> getActivite() {
        return ResponseEntity.ok(dashboardService.getActiviteRecente());
    }

    /** Endpoint unique retournant toutes les stats en un seul appel */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getDashboardComplet() {
        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("global",     dashboardService.getStatsGlobales());
        dashboard.put("dossiers",   dashboardService.getStatsDossiers());
        dashboard.put("ot",         dashboardService.getStatsOT());
        dashboard.put("clients",    dashboardService.getStatsClients());
        dashboard.put("mouvements", dashboardService.getStatsMouvements());
        dashboard.put("conteneurs", dashboardService.getStatsConteneurs());
        dashboard.put("documents",  dashboardService.getStatsDocuments());
        dashboard.put("activite",   dashboardService.getActiviteRecente());
        return ResponseEntity.ok(dashboard);
    }
}
