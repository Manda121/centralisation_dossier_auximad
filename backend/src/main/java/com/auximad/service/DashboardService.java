package com.auximad.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DashboardService {

    private final JdbcTemplate jdbc;

    public DashboardService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // =========================================================
    // STATS GLOBALES
    // =========================================================
    public Map<String, Object> getStatsGlobales() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalDossiers",    count("SELECT COUNT(*) FROM dossier"));
        stats.put("totalOT",          count("SELECT COUNT(*) FROM ordre_transit"));
        stats.put("totalClients",     count("SELECT COUNT(*) FROM clients"));
        stats.put("totalMouvements",  count("SELECT COUNT(*) FROM mvt_dossier"));
        stats.put("totalConteneurs",  count("SELECT COUNT(*) FROM conteneur"));
        stats.put("totalDeclarations",count("SELECT COUNT(*) FROM declaration"));
        stats.put("totalDocuments",   count("SELECT COUNT(*) FROM document"));
        stats.put("totalUtilisateurs",count("SELECT COUNT(*) FROM utilisateur"));
        return stats;
    }

    // =========================================================
    // STATS DOSSIERS
    // =========================================================
    public Map<String, Object> getStatsDossiers() {
        Map<String, Object> stats = new LinkedHashMap<>();

        // Par statut
        List<Map<String, Object>> parStatut = jdbc.queryForList(
            "SELECT s.nom AS label, COUNT(d.id) AS valeur " +
            "FROM statut_dossier s LEFT JOIN dossier d ON d.id_statut = s.id " +
            "GROUP BY s.nom ORDER BY valeur DESC");
        stats.put("parStatut", parStatut);

        // Par phase
        List<Map<String, Object>> parPhase = jdbc.queryForList(
            "SELECT p.nom AS label, COUNT(d.id) AS valeur " +
            "FROM phase_dossier p LEFT JOIN dossier d ON d.id_phase = p.id " +
            "GROUP BY p.nom ORDER BY valeur DESC");
        stats.put("parPhase", parPhase);

        // Par type de production
        List<Map<String, Object>> parType = jdbc.queryForList(
            "SELECT tp.nom AS label, mt.nom AS mode, COUNT(d.id) AS valeur " +
            "FROM type_production tp " +
            "JOIN mode_transport mt ON mt.id = tp.id_mode_transport " +
            "LEFT JOIN dossier d ON d.id_type_production = tp.id " +
            "GROUP BY tp.nom, mt.nom ORDER BY valeur DESC");
        stats.put("parTypeProduction", parType);

        // Par agence (top 10)
        List<Map<String, Object>> parAgence = jdbc.queryForList(
            "SELECT a.nom AS label, COUNT(d.id) AS valeur " +
            "FROM agence a LEFT JOIN dossier d ON d.id_agence = a.id " +
            "GROUP BY a.nom ORDER BY valeur DESC LIMIT 10");
        stats.put("parAgence", parAgence);

        // Dossiers créés sur les 12 derniers mois calendaires, mois sans dossier inclus.
        List<Map<String, Object>> evolution = jdbc.queryForList(
            "SELECT TO_CHAR(m.debut, 'YYYY-MM') AS mois, COUNT(d.id) AS valeur " +
            "FROM GENERATE_SERIES(DATE_TRUNC('month', NOW()) - INTERVAL '11 months', " +
            "DATE_TRUNC('month', NOW()), INTERVAL '1 month') AS m(debut) " +
            "LEFT JOIN dossier d ON d.date_creation >= m.debut " +
            "AND d.date_creation < m.debut + INTERVAL '1 month' " +
            "AND d.date_creation <= NOW() " +
            "GROUP BY m.debut ORDER BY m.debut");
        stats.put("evolutionMensuelle", evolution);

        return stats;
    }

    // =========================================================
    // STATS ORDRES DE TRANSIT
    // =========================================================
    public Map<String, Object> getStatsOT() {
        Map<String, Object> stats = new LinkedHashMap<>();

        List<Map<String, Object>> parStatut = jdbc.queryForList(
            "SELECT s.nom AS label, COUNT(ot.id) AS valeur " +
            "FROM statut_ot s LEFT JOIN ordre_transit ot ON ot.id_statut_ot = s.id " +
            "GROUP BY s.nom ORDER BY valeur DESC");
        stats.put("parStatut", parStatut);

        List<Map<String, Object>> parType = jdbc.queryForList(
            "SELECT tp.nom AS label, mt.nom AS mode, COUNT(ot.id) AS valeur " +
            "FROM type_production tp " +
            "JOIN mode_transport mt ON mt.id = tp.id_mode_transport " +
            "LEFT JOIN ordre_transit ot ON ot.id_type_production = tp.id " +
            "GROUP BY tp.nom, mt.nom ORDER BY valeur DESC");
        stats.put("parTypeProduction", parType);

        List<Map<String, Object>> parRegime = jdbc.queryForList(
            "SELECT tr.nom AS label, COUNT(ot.id) AS valeur " +
            "FROM type_regime tr LEFT JOIN ordre_transit ot ON ot.id_type_regime = tr.id " +
            "GROUP BY tr.nom ORDER BY valeur DESC");
        stats.put("parRegime", parRegime);

        List<Map<String, Object>> parAgence = jdbc.queryForList(
            "SELECT a.nom AS label, COUNT(ot.id) AS valeur " +
            "FROM agence a LEFT JOIN ordre_transit ot ON ot.id_agence = a.id " +
            "GROUP BY a.nom ORDER BY valeur DESC LIMIT 10");
        stats.put("parAgence", parAgence);

        return stats;
    }

    // =========================================================
    // STATS CLIENTS
    // =========================================================
    public Map<String, Object> getStatsClients() {
        Map<String, Object> stats = new LinkedHashMap<>();

        List<Map<String, Object>> parType = jdbc.queryForList(
            "SELECT tc.nom AS label, COUNT(c.id) AS valeur " +
            "FROM type_client tc LEFT JOIN clients c ON c.id_type_client = tc.id " +
            "GROUP BY tc.nom ORDER BY valeur DESC");
        stats.put("parType", parType);

        List<Map<String, Object>> parPays = jdbc.queryForList(
            "SELECT p.nom AS label, COUNT(c.id) AS valeur " +
            "FROM pays p LEFT JOIN clients c ON c.id_pays = p.id " +
            "GROUP BY p.nom ORDER BY valeur DESC LIMIT 10");
        stats.put("parPays", parPays);

        return stats;
    }

    // =========================================================
    // STATS MOUVEMENTS
    // =========================================================
    public Map<String, Object> getStatsMouvements() {
        Map<String, Object> stats = new LinkedHashMap<>();

        List<Map<String, Object>> parOperation = jdbc.queryForList(
            "SELECT op.description AS label, COUNT(m.id) AS valeur " +
            "FROM operation_dossier op LEFT JOIN mvt_dossier m ON m.id_operation = op.id " +
            "GROUP BY op.description ORDER BY valeur DESC LIMIT 10");
        stats.put("parOperation", parOperation);

        List<Map<String, Object>> parPhase = jdbc.queryForList(
            "SELECT ph.nom AS label, COUNT(m.id) AS valeur " +
            "FROM phase_dossier ph " +
            "LEFT JOIN operation_dossier op ON op.id_phase = ph.id " +
            "LEFT JOIN mvt_dossier m ON m.id_operation = op.id " +
            "GROUP BY ph.nom ORDER BY valeur DESC");
        stats.put("parPhase", parPhase);

        // Évolution hebdomadaire des mouvements (8 dernières semaines)
        List<Map<String, Object>> evolution = jdbc.queryForList(
            "SELECT TO_CHAR(DATE_TRUNC('week', date_mvt), 'YYYY-MM-DD') AS semaine, COUNT(*) AS valeur " +
            "FROM mvt_dossier " +
            "WHERE date_mvt >= NOW() - INTERVAL '8 weeks' " +
            "GROUP BY semaine ORDER BY semaine");
        stats.put("evolutionHebdo", evolution);

        return stats;
    }

    // =========================================================
    // STATS CONTENEURS
    // =========================================================
    public Map<String, Object> getStatsConteneurs() {
        Map<String, Object> stats = new LinkedHashMap<>();

        List<Map<String, Object>> parType = jdbc.queryForList(
            "SELECT tc.nom AS label, COUNT(c.id) AS valeur " +
            "FROM type_container tc LEFT JOIN conteneur c ON c.id_type_container = tc.id " +
            "GROUP BY tc.nom ORDER BY valeur DESC");
        stats.put("parType", parType);

        return stats;
    }

    // =========================================================
    // STATS DOCUMENTS
    // =========================================================
    public Map<String, Object> getStatsDocuments() {
        Map<String, Object> stats = new LinkedHashMap<>();

        List<Map<String, Object>> parType = jdbc.queryForList(
            "SELECT td.nom AS label, COUNT(d.id) AS valeur " +
            "FROM type_document td LEFT JOIN document d ON d.id_type_document = td.id " +
            "GROUP BY td.nom ORDER BY valeur DESC LIMIT 12");
        stats.put("parType", parType);

        return stats;
    }

    // =========================================================
    // ACTIVITE RECENTE
    // =========================================================
    public List<Map<String, Object>> getActiviteRecente() {
        return jdbc.queryForList(
            "SELECT m.id, m.date_mvt AS date_mouvement, m.commentaire, " +
            "u.nom_utilisateur, u.nom AS nom_user, " +
            "op.description AS operation, " +
            "ph.nom AS phase, " +
            "d.numero_dossier " +
            "FROM mvt_dossier m " +
            "JOIN utilisateur u ON u.id = m.id_utilisateur " +
            "JOIN operation_dossier op ON op.id = m.id_operation " +
            "JOIN phase_dossier ph ON ph.id = op.id_phase " +
            "JOIN dossier d ON d.id = m.id_dossier " +
            "ORDER BY m.date_mvt DESC LIMIT 10");
    }

    // =========================================================
    // HELPER
    // =========================================================
    private long count(String sql) {
        Long result = jdbc.queryForObject(sql, Long.class);
        return result != null ? result : 0L;
    }
}
