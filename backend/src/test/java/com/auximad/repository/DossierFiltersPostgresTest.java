package com.auximad.repository;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.lang.reflect.Method;
import java.sql.Types;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** SQL is exercised in an isolated schema without changing application data. */
@EnabledIfEnvironmentVariable(named = "AUXIMAD_TEST_DATABASE_URL", matches = ".+")
class DossierFiltersPostgresTest {
    private String schema;
    private JdbcTemplate admin;
    private NamedParameterJdbcTemplate jdbc;
    private Method search;

    @BeforeEach void setup() {
        var ds = new DriverManagerDataSource(System.getenv("AUXIMAD_TEST_DATABASE_URL"),
                System.getenv("AUXIMAD_TEST_DATABASE_USER"), System.getenv("AUXIMAD_TEST_DATABASE_PASSWORD"));
        admin = new JdbcTemplate(ds);
        schema = "dossier_filters_test_" + UUID.randomUUID().toString().replace("-", "");
        admin.execute("CREATE SCHEMA " + schema);
        var isolated = new DriverManagerDataSource(ds.getUrl(), ds.getUsername(), ds.getPassword());
        var properties = new Properties();
        properties.setProperty("currentSchema", schema);
        isolated.setConnectionProperties(properties);
        var raw = new JdbcTemplate(isolated);
        jdbc = new NamedParameterJdbcTemplate(isolated);
        search = Arrays.stream(DossierRepository.class.getMethods())
                .filter(m -> m.getName().equals("searchDossiersFiltres")).findFirst().orElseThrow();
        raw.execute("""
            CREATE TABLE clients(id int, nom text, prenom text, cp_client text);
            CREATE TABLE phase_dossier(id int, nom text);
            CREATE TABLE statut_dossier(id int, nom text);
            CREATE TABLE ordre_transit(id int, numero_ordre_transit int);
            CREATE TABLE dossier_interagence(id_dossier_mere int, id_sous_dossier int);
            CREATE TABLE dossier(id int, numero_dossier text, id_client int, id_agence int,
                id_phase int, id_statut int, id_type_production int, id_ordre_transit int,
                est_groupage boolean, date_creation timestamp, id_pays_origine int,
                id_pays_destination int, id_inconterme int, pour_compte text, depart text,
                adresse_livraison text, lieu_paiement_fret text, est_prepaye boolean,
                est_provisionne boolean, provision_demandee numeric, echeance timestamp,
                date_livraison timestamp);
            INSERT INTO clients VALUES(1, 'Martin', 'Anne', '001'), (2, 'Durand', 'Paul', '002');
            INSERT INTO ordre_transit VALUES(1, 18), (2, 19);
            INSERT INTO dossier VALUES
                (1, 'DOS-001', 1, 1, 1, 1, 1, 1, false, '2026-10-02 14:00', 1, 2, 1,
                 'Acme', 'Toamasina', 'Antananarivo', 'Paris', false, true, 150,
                 '2026-10-02 23:59:59', '2026-10-02 18:00'),
                (2, 'DOS-002', 1, 2, 1, 1, 1, 1, true, '2026-10-03 10:00', 2, 1, 2,
                 'Autre', 'Paris', 'Toamasina', 'Lyon', true, false, 50,
                 '2026-10-03 00:00', '2026-10-03 00:00'),
                (3, 'DOS-003', 2, 1, 2, 2, 2, 2, false, '2026-10-02 10:00', 2, 1, 2,
                 NULL, NULL, NULL, NULL, false, false, 0, NULL, NULL);
            INSERT INTO dossier_interagence VALUES(1, 2);
            """);
    }

    @AfterEach void cleanup() {
        if (schema != null) admin.execute("DROP SCHEMA " + schema + " CASCADE");
    }

    private void expect(List<Integer> ids, Map<String, Object> filters) {
        var params = new MapSqlParameterSource();
        for (var parameter : search.getParameters()) {
            var annotation = parameter.getAnnotation(Param.class);
            if (annotation == null) continue;
            int type = parameter.getType() == Integer.class ? Types.INTEGER
                    : parameter.getType() == Boolean.class ? Types.BOOLEAN
                    : parameter.getType() == java.math.BigDecimal.class ? Types.NUMERIC : Types.VARCHAR;
            params.addValue(annotation.value(), filters.get(annotation.value()), type);
        }
        var query = search.getAnnotation(Query.class);
        assertEquals(ids, jdbc.query(query.value() + " ORDER BY d.id", params,
                (rs, row) -> rs.getInt("id")));
        assertEquals((long) ids.size(), jdbc.queryForObject(query.countQuery(), params, Long.class));
    }

    @Test void optionalFiltersKeepAllResultsAndPaginationCount() {
        expect(List.of(1, 2, 3), Map.of());
        expect(List.of(1, 2), Map.of("clientNom", "MARTIN", "cpClient", "001"));
        expect(List.of(1), Map.of("clientNom", "anne", "cpClient", "001", "agenceId", 1));
        expect(List.of(), Map.of("clientNom", "Martin", "cpClient", "002"));
    }

    @Test void detailFieldsCanBeCombinedIncludingFalseAndAmounts() {
        var filters = new HashMap<String, Object>();
        filters.put("numeroDossier", "001"); filters.put("numeroOt", 18);
        filters.put("paysOrigineId", 1); filters.put("paysDestinationId", 2);
        filters.put("incontermeId", 1); filters.put("pourCompte", "ACME");
        filters.put("depart", "toama"); filters.put("adresseLivraison", "antana");
        filters.put("lieuPaiementFret", "paris"); filters.put("estPrepaye", false);
        filters.put("estProvisionne", true); filters.put("provisionMin", 100);
        filters.put("provisionMax", 150); filters.put("typeProductionId", 1);
        expect(List.of(1), filters);
        expect(List.of(1, 3), Map.of("estPrepaye", false));
        expect(List.of(), Map.of("clientNom", "%"));
    }

    @Test void datesIncludeFinalDayAndExcludeNextMidnight() {
        expect(List.of(1), Map.of("echeanceMin", "2026-10-02", "echeanceMax", "2026-10-02"));
        expect(List.of(1), Map.of("dateLivraisonMin", "2026-10-02", "dateLivraisonMax", "2026-10-02"));
        expect(List.of(1, 3), Map.of("dateMin", "2026-10-02T00:00:00", "dateMax", "2026-10-02T23:59:59.999"));
    }

    @Test void existingFiltersRemainCombinedWithNewFilters() {
        expect(List.of(1), Map.of("typeDossier", "MERE", "cpClient", "001"));
        expect(List.of(2), Map.of("typeDossier", "SOUS_DOSSIER", "agenceId", 2));
        expect(List.of(1), Map.of("phaseId", 1, "statutId", 1, "estGroupage", false, "cpClient", "001"));
    }
}