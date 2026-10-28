package com.auximad.service;

import com.auximad.controller.DashboardController;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Exercises dashboard SQL in an isolated schema without changing application data. */
@EnabledIfEnvironmentVariable(named = "AUXIMAD_TEST_DATABASE_URL", matches = ".+")
class DashboardServicePostgresTest {
    private String schema;
    private JdbcTemplate admin, jdbc;
    private MockMvc mvc;

    @BeforeEach void setup() {
        var base = new DriverManagerDataSource(System.getenv("AUXIMAD_TEST_DATABASE_URL"),
                System.getenv("AUXIMAD_TEST_DATABASE_USER"), System.getenv("AUXIMAD_TEST_DATABASE_PASSWORD"));
        admin = new JdbcTemplate(base);
        schema = "dashboard_test_" + UUID.randomUUID().toString().replace("-", "");
        admin.execute("CREATE SCHEMA " + schema);
        var isolated = new DriverManagerDataSource(base.getUrl(), base.getUsername(), base.getPassword());
        var properties = new Properties();
        properties.setProperty("currentSchema", schema);
        isolated.setConnectionProperties(properties);
        jdbc = new JdbcTemplate(isolated);
        jdbc.execute("""
            CREATE TABLE statut_dossier(id int PRIMARY KEY, nom text);
            CREATE TABLE phase_dossier(id int PRIMARY KEY, nom text);
            CREATE TABLE mode_transport(id int PRIMARY KEY, nom text);
            CREATE TABLE type_production(id int PRIMARY KEY, nom text, id_mode_transport int);
            CREATE TABLE agence(id int PRIMARY KEY, nom text);
            CREATE TABLE statut_ot(id int PRIMARY KEY, nom text);
            CREATE TABLE type_regime(id int PRIMARY KEY, nom text);
            CREATE TABLE type_client(id int PRIMARY KEY, nom text);
            CREATE TABLE pays(id int PRIMARY KEY, nom text);
            CREATE TABLE type_container(id int PRIMARY KEY, nom text);
            CREATE TABLE type_document(id int PRIMARY KEY, nom text, id_type_production int,
                id_nature_marchandise int, est_obligatoire boolean);
            CREATE TABLE dossier(id int PRIMARY KEY, numero_dossier text, id_client int, id_ordre_transit int,
                id_utilisateur int, id_statut int, id_phase int, id_type_production int, id_agence int,
                echeance timestamptz, date_creation timestamptz DEFAULT NOW());
            CREATE TABLE ordre_transit(id int PRIMARY KEY, id_statut_ot int,
                id_type_production int, id_type_regime int, id_agence int, id_nature_marchandise int);
            CREATE TABLE clients(id int PRIMARY KEY, nom text, prenom text, id_type_client int, id_pays int);
            CREATE TABLE operation_dossier(id int PRIMARY KEY, description text, id_phase int);
            CREATE TABLE utilisateur(id int PRIMARY KEY, nom_utilisateur text, nom text);
            CREATE TABLE mvt_dossier(id int PRIMARY KEY, id_dossier int, id_utilisateur int,
                id_operation int, commentaire text, date_mvt timestamptz, est_annule boolean DEFAULT false);
            CREATE TABLE conteneur(id int PRIMARY KEY, id_type_container int);
            CREATE TABLE declaration(id int PRIMARY KEY);
            CREATE TABLE document(id int PRIMARY KEY, id_dossier int, id_type_document int, est_actif boolean);
            CREATE TABLE demande_interagence(id int PRIMARY KEY, id_dossier int, id_agence_demandeuse int,
                id_agence_destinataire int, id_utilisateur_demande int, motif text, statut text,
                date_demande timestamptz DEFAULT NOW());
            """);
        mvc = MockMvcBuilders.standaloneSetup(new DashboardController(new DashboardService(jdbc))).build();
    }

    @AfterEach void cleanup() {
        SecurityContextHolder.clearContext();
        if (schema != null && schema.matches("dashboard_test_[a-f0-9]{32}")) {
            admin.execute("DROP SCHEMA " + schema + " CASCADE");
        }
    }

    private void authentifier(Integer idAgence, int niveauAutorisation) {
        Claims claims = Jwts.claims()
                .add("idAgence", idAgence)
                .add("niveauAutorisation", niveauAutorisation)
                .build();
        var auth = new UsernamePasswordAuthenticationToken("utilisateur", null,
                AuthorityUtils.NO_AUTHORITIES);
        auth.setDetails(claims);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test void completeDashboardLoadsWithAnEmptyDatabase() throws Exception {
        mvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.global.totalMouvements").value(0))
                .andExpect(jsonPath("$.dossiers.parStatut").isEmpty())
                .andExpect(jsonPath("$.dossiers.evolutionMensuelle.length()").value(12))
                .andExpect(jsonPath("$.dossiers.evolutionMensuelle[0].valeur").value(0))
                .andExpect(jsonPath("$.ot.parStatut").isEmpty())
                .andExpect(jsonPath("$.clients.parType").isEmpty())
                .andExpect(jsonPath("$.mouvements.evolutionHebdo").isEmpty())
                .andExpect(jsonPath("$.conteneurs.parType").isEmpty())
                .andExpect(jsonPath("$.documents.parType").isEmpty())
                .andExpect(jsonPath("$.activite").isEmpty());
    }

    @Test void monthlyEvolutionCountsCreationDatesAndIncludesEmptyMonths() throws Exception {
        jdbc.execute("""
            INSERT INTO dossier(id, date_creation, echeance) VALUES
                (1, DATE_TRUNC('month', NOW()), NOW() + INTERVAL '1 year'),
                (2, DATE_TRUNC('month', NOW()), NULL),
                (3, DATE_TRUNC('month', NOW()) - INTERVAL '2 months', NOW()),
                (4, DATE_TRUNC('month', NOW()) - INTERVAL '12 months', NOW()),
                (5, DATE_TRUNC('month', NOW()) + INTERVAL '1 month', NOW()),
                (6, NOW() + INTERVAL '1 hour', NOW());
            """);
        mvc.perform(get("/api/dashboard/module/dossiers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evolutionMensuelle.length()").value(12))
                .andExpect(jsonPath("$.evolutionMensuelle[0].mois").value(jdbc.queryForObject(
                        "SELECT TO_CHAR(DATE_TRUNC('month', NOW()) - INTERVAL '11 months', 'YYYY-MM')", String.class)))
                .andExpect(jsonPath("$.evolutionMensuelle[9].valeur").value(1))
                .andExpect(jsonPath("$.evolutionMensuelle[10].valeur").value(0))
                .andExpect(jsonPath("$.evolutionMensuelle[11].mois").value(jdbc.queryForObject(
                        "SELECT TO_CHAR(NOW(), 'YYYY-MM')", String.class)))
                .andExpect(jsonPath("$.evolutionMensuelle[11].valeur").value(2));
    }

    @Test void weeklyEvolutionUsesMovementDateAndExcludesOlderMovements() throws Exception {
        jdbc.execute("""
            INSERT INTO mvt_dossier(id, date_mvt) VALUES
                (1, NOW()), (2, NOW()), (3, NOW() - INTERVAL '9 weeks');
            """);
        mvc.perform(get("/api/dashboard/module/mouvements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evolutionHebdo.length()").value(1))
                .andExpect(jsonPath("$.evolutionHebdo[0].valeur").value(2))
                .andExpect(jsonPath("$.evolutionHebdo[0].semaine")
                        .value(jdbc.queryForObject("SELECT TO_CHAR(DATE_TRUNC('week', NOW()), 'YYYY-MM-DD')", String.class)));
    }

    @Test void completeDashboardReturnsLatestTenActivitiesWithTheFrontendDateField() throws Exception {
        jdbc.execute("""
            INSERT INTO phase_dossier VALUES(1, 'Transit');
            INSERT INTO operation_dossier VALUES(1, 'Reception', 1);
            INSERT INTO utilisateur VALUES(1, 'agent', 'Agent');
            INSERT INTO dossier(id, numero_dossier) VALUES(1, 'DOS-001');
            INSERT INTO mvt_dossier(id, id_dossier, id_utilisateur, id_operation, commentaire, date_mvt)
                SELECT n, 1, 1, 1, 'Activite', NOW() - n * INTERVAL '1 minute'
                FROM generate_series(1, 12) AS n;
            """);
        mvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.global.totalMouvements").value(12))
                .andExpect(jsonPath("$.activite.length()").value(10))
                .andExpect(jsonPath("$.activite[0].id").value(1))
                .andExpect(jsonPath("$.activite[9].id").value(10))
                .andExpect(jsonPath("$.activite[0].date_mouvement").isNotEmpty())
                .andExpect(jsonPath("$.activite[0].numero_dossier").value("DOS-001"))
                .andExpect(jsonPath("$.activite[0].nom_utilisateur").value("agent"))
                .andExpect(jsonPath("$.activite[0].operation").value("Reception"))
                .andExpect(jsonPath("$.activite[0].phase").value("Transit"));
        mvc.perform(get("/api/dashboard/activite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date_mouvement").isNotEmpty());
    }

    @Test void dossierDashboardReportsKpiChartsPrioritairesAndActivities() throws Exception {
        jdbc.execute("""
            INSERT INTO agence VALUES(1, 'TNR'), (2, 'TMA');
            INSERT INTO statut_dossier VALUES(1, 'Ouvert'), (2, 'Clôturé'), (3, 'Annulé');
            INSERT INTO phase_dossier VALUES(1, 'PREPARATION'), (2, 'COMPTABILITE');
            INSERT INTO type_production VALUES(1, 'import', NULL);
            INSERT INTO clients VALUES(1, 'Martin', 'Anne', NULL, NULL);
            INSERT INTO dossier(id, numero_dossier, id_client, id_statut, id_phase,
                id_type_production, id_agence, echeance, date_creation) VALUES
                (1, 'DOS-001', 1, 1, 1, 1, 1, NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day'),
                (2, 'DOS-002', 1, 1, 2, 1, 2, NOW() + INTERVAL '3 days', NOW() - INTERVAL '2 days'),
                (3, 'DOS-003', 1, 2, 2, 1, 1, NOW() - INTERVAL '30 days', NOW() - INTERVAL '3 days'),
                (4, 'DOS-004', 1, 3, 1, 1, 1, NOW() - INTERVAL '1 hour', NOW() - INTERVAL '4 days');
            INSERT INTO type_document VALUES(1, 'Facture', 1, NULL, true);
            INSERT INTO document VALUES(1, 2, 1, true), (2, 3, 1, true), (3, 4, 1, true);
            INSERT INTO demande_interagence(id, id_dossier, id_utilisateur_demande, motif, statut, date_demande) VALUES
                (1, 2, 1, 'Docs manquants', 'EN_ATTENTE', NOW() - INTERVAL '1 hour'),
                (2, 3, 1, 'Autre motif', 'ACCEPTEE', NOW() - INTERVAL '2 hours');
            """);

        mvc.perform(get("/api/dashboard/dossiers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpi.enCours").value(2))
                .andExpect(jsonPath("$.kpi.clotures").value(1))
                .andExpect(jsonPath("$.kpi.documentsIncomplets").value(1))
                .andExpect(jsonPath("$.kpi.echeancesDepassees").value(1))
                .andExpect(jsonPath("$.kpi.demandesEnAttente").value(1))
                .andExpect(jsonPath("$.kpi.dossiersPeriode").value(4))
                .andExpect(jsonPath("$.parPhase.length()").value(2))
                .andExpect(jsonPath("$.parAgence.length()").value(2))
                .andExpect(jsonPath("$.evolution.granularite").value("MOIS"))
                .andExpect(jsonPath("$.prioritaires[0].numero_dossier").value("DOS-001"))
                .andExpect(jsonPath("$.prioritaires[0].echeance_depassee").value(true))
                .andExpect(jsonPath("$.prioritaires[0].docs_manquants").value(true))
                .andExpect(jsonPath("$.activites[0].type").value("DEMANDE"))
                .andExpect(jsonPath("$.activites.length()").value(6));
    }

    @Test void dossierDashboardIsRestrictedToTheUserAgencyWithoutAllReadScope() throws Exception {
        jdbc.execute("""
            INSERT INTO agence VALUES(1, 'TNR'), (2, 'TMA');
            INSERT INTO statut_dossier VALUES(1, 'Ouvert');
            INSERT INTO phase_dossier VALUES(1, 'PREPARATION');
            INSERT INTO type_production VALUES(1, 'import', NULL);
            INSERT INTO clients VALUES(1, 'Martin', 'Anne', NULL, NULL);
            INSERT INTO dossier(id, numero_dossier, id_client, id_statut, id_phase,
                id_type_production, id_agence, echeance) VALUES
                (1, 'DOS-001', 1, 1, 1, 1, 1, NOW() + INTERVAL '5 days'),
                (2, 'DOS-002', 1, 1, 1, 1, 2, NOW() + INTERVAL '10 days');
            INSERT INTO demande_interagence(id, id_dossier, id_utilisateur_demande, motif, statut, date_demande) VALUES
                (1, 2, 1, 'Docs manquants', 'EN_ATTENTE', NOW() - INTERVAL '1 hour');
            """);
        // Niveau 110 = lecture propre sur le contexte OT : le périmètre agence est imposé (agence 2)
        authentifier(2, 110);
        mvc.perform(get("/api/dashboard/dossiers").param("agenceId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perimetre.agenceId").value(2))
                .andExpect(jsonPath("$.kpi.enCours").value(1))
                .andExpect(jsonPath("$.parAgence.length()").value(1))
                .andExpect(jsonPath("$.parAgence[0].label").value("TMA"))
                .andExpect(jsonPath("$.prioritaires[0].numero_dossier").value("DOS-002"));
    }
}
