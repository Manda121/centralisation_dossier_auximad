package com.auximad.controller;

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
