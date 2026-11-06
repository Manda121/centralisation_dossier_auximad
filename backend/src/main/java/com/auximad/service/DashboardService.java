package com.auximad.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DashboardService {

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;

    // Schémas SQL communs. Le nommage des statuts de clôture/annulation peut contenir
    // des accents : on utilise des expressions régulières ASCII ('^cl.?tur' couvre
    // "Cloture" comme "Clôturé").
    private static final String SCOPE_DOSSIER =
            " (CAST(:agenceId AS int) IS NULL OR d.id_agence = :agenceId) " +
            " AND (CAST(:typeProductionId AS int) IS NULL OR d.id_type_production = :typeProductionId) ";

    private static final String DOSSIER_ACTIF =
            " NOT EXISTS (SELECT 1 FROM statut_dossier sx " +
            "   WHERE sx.id = d.id_statut " +
            "   AND (lower(sx.nom) ~ '^cl.?tur' OR lower(sx.nom) ~ '^ann.?l')) ";

    private static final String DOSSIER_CLOTURE =
            " EXISTS (SELECT 1 FROM statut_dossier sx " +
            "   WHERE sx.id = d.id_statut AND lower(sx.nom) ~ '^cl.?tur') ";

    private static final String DOCS_OBLIGATOIRES_MANQUANTS =
            " EXISTS (SELECT 1 FROM type_document td " +
            "   WHERE td.id_type_production = d.id_type_production " +
            "   AND (td.id_nature_marchandise IS NULL OR td.id_nature_marchandise = ot.id_nature_marchandise) " +
            "   AND td.est_obligatoire = true " +
            "   AND NOT EXISTS (SELECT 1 FROM document doc " +
            "     WHERE doc.id_dossier = d.id AND doc.id_type_document = td.id AND doc.est_actif = true)) ";

    private static final String DEMANDE_EN_ATTENTE =
            " EXISTS (SELECT 1 FROM demande_interagence dge " +
            "   WHERE dge.id_dossier = d.id AND dge.statut = 'EN_ATTENTE') ";

    public DashboardService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.named = new NamedParameterJdbcTemplate(jdbc.getDataSource());
    }

    // =========================================================
    // VUE DOSSIERS DU TABLEAU DE BORD (KPI, graphiques, prioritaires, activités)
    // =========================================================
    public Map<String, Object> getVueDossiers(Integer agenceId, Integer typeProductionId,
                                              String dateDebut, String dateFin) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("agenceId", agenceId)
                .addValue("typeProductionId", typeProductionId);

        LocalDate debut = parserDate(dateDebut);
        LocalDate fin = parserDate(dateFin);
        LocalDate aujourdhui = LocalDate.now();
        if (fin == null) fin = aujourdhui;

        Map<String, Object> result = new LinkedHashMap<>();

        // ── Filtres globaux renvoyés pour information (périmètre effectif côté serveur)
        Map<String, Object> perimetre = new LinkedHashMap<>();
        perimetre.put("agenceId", agenceId);
        perimetre.put("typeProductionId", typeProductionId);
        if (debut != null) perimetre.put("debut", debut.toString());
        perimetre.put("fin", fin.toString());
        result.put("perimetre", perimetre);

        // ── KPI situationnels (périmètre agence + type de production, sans limite de période)
        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("enCours", count("""
                SELECT COUNT(*) FROM dossier d
                WHERE %s AND %s""".formatted(SCOPE_DOSSIER, DOSSIER_ACTIF), params));
        kpi.put("clotures", count("""
                SELECT COUNT(*) FROM dossier d
                WHERE %s AND %s""".formatted(SCOPE_DOSSIER, DOSSIER_CLOTURE), params));
        kpi.put("documentsIncomplets", count("""
                SELECT COUNT(*) FROM (
                  SELECT DISTINCT d.id FROM dossier d
                  LEFT JOIN ordre_transit ot ON ot.id = d.id_ordre_transit
                  JOIN type_document td ON td.id_type_production = d.id_type_production
                    AND (td.id_nature_marchandise IS NULL OR td.id_nature_marchandise = ot.id_nature_marchandise)
                    AND td.est_obligatoire = true
                  WHERE %s
                    AND NOT EXISTS (SELECT 1 FROM document doc
                      WHERE doc.id_dossier = d.id AND doc.id_type_document = td.id AND doc.est_actif = true)
                ) manquants""".formatted(SCOPE_DOSSIER), params));
        kpi.put("echeancesDepassees", count("""
                SELECT COUNT(*) FROM dossier d
                WHERE %s AND %s
                  AND d.echeance IS NOT NULL AND d.echeance < NOW()""".formatted(SCOPE_DOSSIER, DOSSIER_ACTIF), params));
        kpi.put("demandesEnAttente", count("""
                SELECT COUNT(DISTINCT dge.id_dossier)
                FROM demande_interagence dge
                JOIN dossier d ON d.id = dge.id_dossier
                WHERE dge.statut = 'EN_ATTENTE' AND %s""".formatted(SCOPE_DOSSIER), params));
        if (debut != null) {
            params.addValue("debutI", debut.atStartOfDay());
            params.addValue("finExcI", fin.plusDays(1).atStartOfDay());
            kpi.put("dossiersPeriode", count("""
                    SELECT COUNT(*) FROM dossier d
                    WHERE %s
                      AND d.date_creation >= :debutI AND d.date_creation < :finExcI""".formatted(SCOPE_DOSSIER), params));
        } else {
            kpi.put("dossiersPeriode", count("SELECT COUNT(*) FROM dossier d WHERE " + SCOPE_DOSSIER, params));
        }
        result.put("kpi", kpi);

        // ── Répartition des dossiers ACTIFS par phase
        result.put("parPhase", named.queryForList("""
                SELECT ph.id AS id, COALESCE(ph.nom, '(Sans phase)') AS label, COUNT(*) AS valeur
                FROM dossier d
                LEFT JOIN phase_dossier ph ON ph.id = d.id_phase
                WHERE %s AND %s
                GROUP BY ph.id, ph.nom
                ORDER BY valeur DESC""".formatted(SCOPE_DOSSIER, DOSSIER_ACTIF), params));

        // ── Tous les dossiers par agence, y compris les dossiers meres clotures.
        // Chaque dossier compte dans sa propre agence, meme si plusieurs partagent un OT.
        result.put("parAgence", named.queryForList("""
                SELECT a.id AS id, a.nom AS label, COUNT(*) AS valeur
                FROM dossier d
                JOIN agence a ON a.id = d.id_agence
                WHERE %s
                GROUP BY a.id, a.nom
                ORDER BY valeur DESC, a.nom, a.id""".formatted(SCOPE_DOSSIER), params));

        // ── Évolution des créations de dossiers sur la période sélectionnée
        result.put("evolution", evolutionCreations(agenceId, typeProductionId, debut, fin));

        // ── Dossiers prioritaires : anomalies en premier
        result.put("prioritaires", named.queryForList("""
                SELECT d.id, d.numero_dossier,
                       TRIM(COALESCE(c.nom, '') || ' ' || COALESCE(c.prenom, '')) AS client,
                       a.nom AS agence, ph.nom AS phase, d.echeance,
                       (d.echeance IS NOT NULL AND d.echeance < NOW()) AS echeance_depassee,
                       (d.echeance IS NOT NULL AND d.echeance >= NOW()
                          AND d.echeance < NOW() + INTERVAL '7 days') AS echeance_proche,
                       %s AS docs_manquants,
                       %s AS demande_attente
                FROM dossier d
                LEFT JOIN ordre_transit ot ON ot.id = d.id_ordre_transit
                LEFT JOIN clients c ON c.id = d.id_client
                JOIN agence a ON a.id = d.id_agence
                LEFT JOIN phase_dossier ph ON ph.id = d.id_phase
                WHERE %s AND %s
                  AND (
                    (d.echeance IS NOT NULL AND d.echeance < NOW() + INTERVAL '7 days')
                    OR %s
                    OR %s
                  )
                ORDER BY
                  (CASE WHEN d.echeance IS NOT NULL AND d.echeance < NOW() THEN 4 ELSE 0 END
                   + CASE WHEN %s THEN 2 ELSE 0 END
                   + CASE WHEN %s THEN 2 ELSE 0 END
                   + CASE WHEN d.echeance IS NOT NULL AND d.echeance >= NOW()
                        AND d.echeance < NOW() + INTERVAL '7 days' THEN 1 ELSE 0 END) DESC,
                  d.echeance ASC NULLS LAST
                LIMIT 12""".formatted(DOCS_OBLIGATOIRES_MANQUANTS, DEMANDE_EN_ATTENTE,
                    SCOPE_DOSSIER, DOSSIER_ACTIF,
                    DOCS_OBLIGATOIRES_MANQUANTS, DEMANDE_EN_ATTENTE,
                    DOCS_OBLIGATOIRES_MANQUANTS, DEMANDE_EN_ATTENTE), params));

        // ── Activités récentes (mouvements, créations, demandes inter-agences)
        result.put("activites", activationsRecentes(agenceId, typeProductionId));

        return result;
    }

    private Map<String, Object> evolutionCreations(Integer agenceId, Integer typeProductionId,
                                                   LocalDate debut, LocalDate fin) {
        LocalDate finEffective = fin != null ? fin : LocalDate.now();
        LocalDate debutEffective = debut != null ? debut : finEffective.minusMonths(11);
        long jours = java.time.temporal.ChronoUnit.DAYS.between(debutEffective, finEffective.plusDays(1));

        // Granularité adaptée à la durée de la période sélectionnée
        String granularite;
        List<LocalDate[]> bornes = new ArrayList<>();
        if (jours <= 31) {
            granularite = "JOUR";
            for (LocalDate d = debutEffective; !d.isAfter(finEffective); d = d.plusDays(1)) {
                bornes.add(new LocalDate[]{d, d});
            }
        } else if (jours <= 120) {
            granularite = "SEMAINE";
            LocalDate cursor = debutEffective.minusDays((debutEffective.getDayOfWeek().getValue() - 1) % 7);
            for (; !cursor.isAfter(finEffective); cursor = cursor.plusWeeks(1)) {
                bornes.add(new LocalDate[]{cursor, cursor.plusDays(6)});
            }
        } else {
            granularite = "MOIS";
            LocalDate cursor = debutEffective.withDayOfMonth(1);
            for (; !cursor.isAfter(finEffective); cursor = cursor.plusMonths(1)) {
                bornes.add(new LocalDate[]{cursor, cursor.plusMonths(1).minusDays(1)});
            }
        }

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("agenceId", agenceId)
                .addValue("typeProductionId", typeProductionId);
        List<Map<String, Object>> lignes = named.queryForList("""
                SELECT TO_CHAR(d.date_creation, 'YYYY-MM-DD') AS jour, COUNT(*) AS valeur
                FROM dossier d
                WHERE %s GROUP BY 1""".formatted(SCOPE_DOSSIER), params);
        Map<String, Long> parJour = new HashMap<>();
        for (Map<String, Object> ligne : lignes) {
            parJour.put((String) ligne.get("jour"), ((Number) ligne.get("valeur")).longValue());
        }

        DateTimeFormatter iso = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<Map<String, Object>> evolution = new ArrayList<>();
        for (LocalDate[] borne : bornes) {
            long total = 0;
            for (LocalDate d = borne[0]; !d.isAfter(borne[1]); d = d.plusDays(1)) {
                total += parJour.getOrDefault(d.format(iso), 0L);
            }
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("debut", borne[0].toString());
            point.put("fin", borne[1].toString());
            switch (granularite) {
                case "JOUR"   -> point.put("label", borne[0].format(DateTimeFormatter.ofPattern("dd/MM")));
                case "SEMAINE"-> point.put("label", "S " + borne[1].format(DateTimeFormatter.ofPattern("dd/MM")));
                default       -> point.put("label", borne[0].format(DateTimeFormatter.ofPattern("MM/yyyy")));
            }
            point.put("valeur", total);
            evolution.add(point);
        }
        Map<String, Object> sortie = new LinkedHashMap<>();
        sortie.put("granularite", granularite);
        sortie.put("points", evolution);
        return sortie;
    }

    private List<Map<String, Object>> activationsRecentes(Integer agenceId, Integer typeProductionId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("agenceId", agenceId)
                .addValue("typeProductionId", typeProductionId);
        String scope = SCOPE_DOSSIER;
        return named.queryForList("""
                SELECT evt.type AS type, evt.id_dossier AS id_dossier, evt.numero_dossier AS numero_dossier,
                       evt.libelle AS libelle, evt.utilisateur AS utilisateur, evt.date_evt AS date_evt
                FROM (
                  (SELECT 'MOUVEMENT' AS type, d.id AS id_dossier, d.numero_dossier, op.description AS libelle,
                          u.nom_utilisateur AS utilisateur, m.date_mvt AS date_evt
                   FROM mvt_dossier m
                   JOIN dossier d ON d.id = m.id_dossier
                   LEFT JOIN operation_dossier op ON op.id = m.id_operation
                   LEFT JOIN utilisateur u ON u.id = m.id_utilisateur
                   WHERE %s AND NOT m.est_annule)
                  UNION ALL
                  (SELECT 'DEMANDE', d.id, d.numero_dossier,
                          'Demande inter-agences : ' || COALESCE(dge.motif, ''),
                          u.nom_utilisateur AS utilisateur, dge.date_demande AS date_evt
                   FROM demande_interagence dge
                   JOIN dossier d ON d.id = dge.id_dossier
                   LEFT JOIN utilisateur u ON u.id = dge.id_utilisateur_demande
                   WHERE %s)
                  UNION ALL
                  (SELECT 'DOSSIER', d.id, d.numero_dossier, 'Creation du dossier',
                          u.nom_utilisateur AS utilisateur, d.date_creation AS date_evt
                   FROM dossier d
                   LEFT JOIN utilisateur u ON u.id = d.id_utilisateur
                   WHERE %s AND d.date_creation >= NOW() - INTERVAL '30 days')
                ) evt
                ORDER BY evt.date_evt DESC
                LIMIT 12""".formatted(scope, scope, scope), params);
    }

    private LocalDate parserDate(String valeur) {
        if (valeur == null || valeur.isBlank()) return null;
        try {
            return LocalDate.parse(valeur.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private long count(String sql, MapSqlParameterSource params) {
        Long result = named.queryForObject(sql, params, Long.class);
        return result != null ? result : 0L;
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
