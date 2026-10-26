package com.auximad.service;

import com.auximad.controller.DashboardController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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
            CREATE TABLE type_document(id int PRIMARY KEY, nom text);
            CREATE TABLE dossier(id int PRIMARY KEY, numero_dossier text, id_statut int,
                id_phase int, id_type_production int, id_agence int, echeance timestamptz,
                date_creation timestamptz DEFAULT NOW());
            CREATE TABLE ordre_transit(id int PRIMARY KEY, id_statut_ot int,
                id_type_production int, id_type_regime int, id_agence int);
            CREATE TABLE clients(id int PRIMARY KEY, id_type_client int, id_pays int);
            CREATE TABLE operation_dossier(id int PRIMARY KEY, description text, id_phase int);
            CREATE TABLE utilisateur(id int PRIMARY KEY, nom_utilisateur text, nom text);
            CREATE TABLE mvt_dossier(id int PRIMARY KEY, id_dossier int, id_utilisateur int,
                id_operation int, commentaire text, date_mvt timestamptz);
            CREATE TABLE conteneur(id int PRIMARY KEY, id_type_container int);
            CREATE TABLE declaration(id int PRIMARY KEY);
            CREATE TABLE document(id int PRIMARY KEY, id_type_document int);
            """);
        mvc = MockMvcBuilders.standaloneSetup(new DashboardController(new DashboardService(jdbc))).build();
    }

    @AfterEach void cleanup() {
        if (schema != null && schema.matches("dashboard_test_[a-f0-9]{32}")) {
            admin.execute("DROP SCHEMA " + schema + " CASCADE");
        }
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
}
