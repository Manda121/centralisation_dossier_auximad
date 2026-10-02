package com.auximad.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Les fixtures sont dans un schéma isolé : aucune donnée métier n'est modifiée. */
@EnabledIfEnvironmentVariable(named="AUXIMAD_TEST_DATABASE_URL", matches=".+")
class SuppressionServicePostgresTest {
    private String schema;
    private JdbcTemplate admin, jdbc;
    private SuppressionService service;
    private TransactionTemplate tx;

    @BeforeEach void setup() {
        DriverManagerDataSource base = datasource();
        admin = new JdbcTemplate(base);
        schema = "suppression_test_" + UUID.randomUUID().toString().replace("-", "");
        admin.execute("CREATE SCHEMA " + schema);
        DriverManagerDataSource isolated = datasource();
        Properties props = new Properties(); props.setProperty("currentSchema", schema);
        isolated.setConnectionProperties(props);
        jdbc = new JdbcTemplate(isolated);
        tx = new TransactionTemplate(new DataSourceTransactionManager(isolated));
        service = new SuppressionService(jdbc);
        jdbc.execute("""
            CREATE TABLE statut_dossier(id serial PRIMARY KEY, nom text UNIQUE, description text);
            INSERT INTO statut_dossier(nom) VALUES ('Ouvert'), ('Clôturé');
            CREATE TABLE clients(id int PRIMARY KEY);
            CREATE TABLE utilisateur(id int PRIMARY KEY);
            CREATE TABLE autorisation(id int PRIMARY KEY, id_utilisateur int REFERENCES utilisateur);
            CREATE TABLE suspension_connexion(id int PRIMARY KEY, id_utilisateur int REFERENCES utilisateur);
            CREATE TABLE agence(id int PRIMARY KEY);
            CREATE TABLE ordre_transit(id int PRIMARY KEY, id_client int REFERENCES clients,
                id_expediteur int REFERENCES clients, id_destinataire int REFERENCES clients);
            CREATE TABLE dossier(id int PRIMARY KEY, id_ordre_transit int REFERENCES ordre_transit,
                id_client int REFERENCES clients, id_statut int REFERENCES statut_dossier);
            CREATE TABLE navire(id int PRIMARY KEY);
            CREATE TABLE contrat_transport(id int PRIMARY KEY, id_ordre_transit int REFERENCES ordre_transit, id_navire int REFERENCES navire);
            CREATE TABLE instruction(id int PRIMARY KEY, id_ordre_transit int REFERENCES ordre_transit);
            CREATE TABLE marchandise(id int PRIMARY KEY, id_ordre_transit int REFERENCES ordre_transit);
            CREATE TABLE document(id int PRIMARY KEY, id_dossier int REFERENCES dossier);
            CREATE TABLE remise(id int PRIMARY KEY, id_dossier int REFERENCES dossier);
            CREATE TABLE operation_dossier(id int PRIMARY KEY);
            CREATE TABLE mvt_dossier(id int PRIMARY KEY, id_dossier int REFERENCES dossier, id_operation int REFERENCES operation_dossier);
            CREATE TABLE document_mvt(id int PRIMARY KEY, id_document int REFERENCES document, id_mvt_dossier int REFERENCES mvt_dossier);
            CREATE TABLE declaration(id int PRIMARY KEY, id_document int REFERENCES document, id_dossier int REFERENCES dossier);
            CREATE TABLE conteneur(id int PRIMARY KEY, id_dossier int REFERENCES dossier);
            CREATE TABLE dossier_conteneur(id_dossier int NOT NULL REFERENCES dossier, id_conteneur int NOT NULL REFERENCES conteneur,
                PRIMARY KEY(id_dossier,id_conteneur));
            CREATE TABLE dossier_interagence(id int PRIMARY KEY, id_dossier_mere int REFERENCES dossier, id_sous_dossier int REFERENCES dossier);
            CREATE TABLE demande_interagence(id int PRIMARY KEY, id_agence_demandeuse int NOT NULL REFERENCES agence,
                id_agence_destinataire int NOT NULL REFERENCES agence, id_utilisateur_demande int NOT NULL REFERENCES utilisateur);
            CREATE TABLE expedition(id int PRIMARY KEY, id_dossier int REFERENCES dossier, id_navire int REFERENCES navire);
            CREATE TABLE groupage(id int PRIMARY KEY, id_dossier int REFERENCES dossier, id_expedition int REFERENCES expedition);
            CREATE TABLE constat(id int PRIMARY KEY, id_dossier int REFERENCES dossier);
            INSERT INTO clients VALUES(1),(2);
            INSERT INTO utilisateur VALUES(1);
            INSERT INTO autorisation VALUES(1,1);
            INSERT INTO suspension_connexion VALUES(1,1);
            INSERT INTO agence VALUES(1),(2);
            INSERT INTO ordre_transit VALUES(1,1,1,1),(2,2,2,2);
            INSERT INTO dossier VALUES(1,1,1,1),(2,2,2,1);
            INSERT INTO navire VALUES(1);
            INSERT INTO contrat_transport VALUES(1,1,1);
            INSERT INTO instruction VALUES(1,1);
            INSERT INTO marchandise VALUES(1,1);
            INSERT INTO document VALUES(1,1);
            INSERT INTO remise VALUES(1,1);
            INSERT INTO operation_dossier VALUES(1),(2);
            INSERT INTO mvt_dossier VALUES(1,1,1),(2,2,2);
            INSERT INTO document_mvt VALUES(1,1,1);
            INSERT INTO declaration VALUES(1,1,1),(2,1,2);
            INSERT INTO conteneur VALUES(1,NULL);
            INSERT INTO dossier_conteneur VALUES(1,1),(2,1);
            INSERT INTO dossier_interagence VALUES(1,1,2);
            INSERT INTO demande_interagence VALUES(1,1,2,1);
            INSERT INTO expedition VALUES(1,1,1);
            INSERT INTO groupage VALUES(1,1,1);
            INSERT INTO constat VALUES(1,1);
            """);
    }
    private DriverManagerDataSource datasource() {
        return new DriverManagerDataSource(System.getenv("AUXIMAD_TEST_DATABASE_URL"),
                System.getenv("AUXIMAD_TEST_DATABASE_USER"), System.getenv("AUXIMAD_TEST_DATABASE_PASSWORD"));
    }
    @AfterEach void cleanup() {
        RequestContextHolder.resetRequestAttributes();
        if (schema != null && schema.matches("suppression_test_[a-f0-9]{32}")) admin.execute("DROP SCHEMA " + schema + " CASCADE");
    }
    private void request(String token) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        if (token != null) req.addHeader(SuppressionService.CONFIRMATION_HEADER, token);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
    }
    private void run(Runnable action) { tx.executeWithoutResult(status -> action.run()); }
    private SuppressionService.Impact preview(Runnable action) {
        request(null);
        return assertThrows(SuppressionConfirmationException.class, () -> run(action)).getImpact();
    }
    private void confirm(Runnable action) {
        var impact = preview(action);
        request(impact.confirmation());
        run(action);
    }
    private int count(String table) { return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class); }
    private Integer integer(String sql) { return jdbc.queryForObject(sql, Integer.class); }

    @Test void previewMakesNoChangesAndListsClosures() {
        var impact = preview(() -> service.supprimer("ordre_transit",1));
        assertEquals(1, impact.dossiersClotures());
        assertTrue(impact.suppressions().stream().anyMatch(d -> d.objet().equals("marchandises") && d.nombre()==1));
        assertEquals(2,count("ordre_transit")); assertEquals(1,integer("SELECT id_ordre_transit FROM dossier WHERE id=1"));
        assertEquals(1,integer("SELECT id_statut FROM dossier WHERE id=1"));
    }
    @Test void deletingOtPreservesAndClosesDossiersWithTheirDocumentsAndMovements() {
        confirm(() -> service.supprimer("ordre_transit",1));
        assertEquals(1,count("ordre_transit")); assertEquals(2,count("dossier"));
        assertNull(integer("SELECT id_ordre_transit FROM dossier WHERE id=1"));
        assertEquals(2,integer("SELECT id_statut FROM dossier WHERE id=1"));
        assertEquals(2,integer("SELECT id_ordre_transit FROM dossier WHERE id=2"));
        assertEquals(0,count("contrat_transport")); assertEquals(0,count("instruction")); assertEquals(0,count("marchandise"));
        assertEquals(1,count("document")); assertEquals(2,count("mvt_dossier")); assertEquals(1,count("document_mvt"));
    }
    @Test void missingClosedStatusIsCreatedOnlyAfterConfirmation() {
        jdbc.update("DELETE FROM statut_dossier WHERE id=2");
        var impact = preview(() -> service.supprimer("ordre_transit",1));
        assertEquals(1,count("statut_dossier"));
        request(impact.confirmation()); run(() -> service.supprimer("ordre_transit",1));
        assertEquals(2,count("statut_dossier"));
        assertNotEquals(1,integer("SELECT id_statut FROM dossier WHERE id=1"));
    }
    @Test void dossierDeletionRemovesDeepDependenciesAndPreservesSharedObjects() {
        confirm(() -> service.supprimer("dossier",1));
        for (String table : List.of("document","remise","document_mvt","dossier_interagence","expedition","groupage","constat")) assertEquals(0,count(table),table);
        assertEquals(1,count("dossier")); assertEquals(1,count("mvt_dossier"));
        assertEquals(1,count("declaration")); assertNull(integer("SELECT id_document FROM declaration WHERE id=2"));
        assertEquals(1,count("conteneur")); assertEquals(1,count("dossier_conteneur")); assertEquals(2,count("ordre_transit"));
    }
    @Test void deletingDocumentRemovesMovementLinksButKeepsDeclaration() {
        confirm(() -> service.supprimer("document",1));
        assertEquals(0,count("document_mvt")); assertEquals(0,count("document"));
        assertEquals(2,count("declaration")); assertEquals(2,count("mvt_dossier"));
        assertNull(integer("SELECT id_document FROM declaration WHERE id=1"));
    }
    @Test void referenceDeletionDetachesSharedObjects() {
        confirm(() -> service.supprimer("navire",1));
        assertEquals(1,count("expedition")); assertEquals(1,count("contrat_transport"));
        assertNull(integer("SELECT id_navire FROM expedition WHERE id=1"));
        assertNull(integer("SELECT id_navire FROM contrat_transport WHERE id=1"));
    }
    @Test void multipleClientReferencesAreAllRemovedWithoutDeletingOt() {
        var impact = preview(() -> service.supprimer("clients",1));
        assertTrue(impact.liensRetires().stream().anyMatch(d -> d.objet().equals("ordres de transit") && d.nombre()==1));
        request(impact.confirmation()); run(() -> service.supprimer("clients",1));
        for (String column : List.of("id_client","id_expediteur","id_destinataire")) assertNull(integer("SELECT " + column + " FROM ordre_transit WHERE id=1"));
        assertEquals(2,count("ordre_transit")); assertEquals(2,count("dossier"));
    }
    @Test void requiredChildrenInBatchAreDeletedOnlyOnce() {
        confirm(() -> service.supprimerLot("agence",List.of(1,2)));
        assertEquals(0,count("agence")); assertEquals(0,count("demande_interagence"));
        assertEquals(2,count("dossier"));
    }
    @Test void operationsBatchKeepsMovementHistory() {
        confirm(() -> service.supprimerLot("operation_dossier",List.of(1,2)));
        assertEquals(0,count("operation_dossier")); assertEquals(2,count("mvt_dossier"));
        assertNull(integer("SELECT id_operation FROM mvt_dossier WHERE id=1"));
    }
    @Test void changedImpactRequiresNewConfirmationWithoutPartialDeletion() {
        var impact = preview(() -> service.supprimer("ordre_transit",1));
        jdbc.update("INSERT INTO instruction VALUES(2,1)");
        request(impact.confirmation());
        var newer = assertThrows(SuppressionConfirmationException.class, () -> run(() -> service.supprimer("ordre_transit",1))).getImpact();
        assertNotEquals(impact.confirmation(),newer.confirmation());
        assertEquals(2,count("instruction")); assertEquals(2,count("ordre_transit"));
        assertEquals(1,integer("SELECT id_statut FROM dossier WHERE id=1"));
    }
    @Test void removingOneContainerAssignmentPreservesOtherAssignments() {
        confirm(() -> service.retirerAssignationConteneur(1,1));
        assertEquals(1,count("conteneur")); assertEquals(1,count("dossier_conteneur"));
        assertEquals(2,integer("SELECT id_dossier FROM dossier_conteneur"));
        confirm(() -> service.retirerAssignationConteneur(1,2));
        assertEquals(0,count("conteneur")); assertEquals(0,count("dossier_conteneur"));
    }
    @Test void failureRollsBackClosureAndAllDependentDeletes() {
        jdbc.execute("""
            CREATE FUNCTION reject_delete() RETURNS trigger LANGUAGE plpgsql AS $$
            BEGIN RAISE EXCEPTION 'Test de rollback' USING ERRCODE='23514'; END; $$;
            CREATE TRIGGER reject_delete BEFORE DELETE ON ordre_transit FOR EACH ROW EXECUTE FUNCTION reject_delete();
            """);
        var impact = preview(() -> service.supprimer("ordre_transit",1));
        request(impact.confirmation());
        assertThrows(RuntimeException.class, () -> run(() -> service.supprimer("ordre_transit",1)));
        assertEquals(2,count("ordre_transit")); assertEquals(1,count("contrat_transport")); assertEquals(1,count("instruction"));
        assertEquals(1,integer("SELECT id_ordre_transit FROM dossier WHERE id=1"));
        assertEquals(1,integer("SELECT id_statut FROM dossier WHERE id=1"));
    }
    @Test void userDeletionRemovesSecurityLinksAndRequiredRequests() {
        confirm(() -> service.supprimer("utilisateur",1));
        assertEquals(0,count("autorisation")); assertEquals(0,count("suspension_connexion")); assertEquals(0,count("demande_interagence"));
        assertEquals(2,count("dossier"));
    }

    @Test void mandatoryOtLinkNeverCausesDossierDeletion() {
        jdbc.execute("ALTER TABLE dossier ALTER COLUMN id_ordre_transit SET NOT NULL");
        request(null);
        assertThrows(IllegalArgumentException.class, () -> run(() -> service.supprimer("ordre_transit",1)));
        assertEquals(2,count("dossier")); assertEquals(2,count("ordre_transit"));
        assertEquals(1,count("contrat_transport"));
    }
}
