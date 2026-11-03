package com.auximad.service;

import com.auximad.entity.*;
import com.auximad.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Mapping et transaction reels dans un schema jetable, sans contexte Spring ni SMTP. */
@EnabledIfEnvironmentVariable(named = "AUXIMAD_TEST_DATABASE_URL", matches = ".+")
class HeritageSousDossierPostgresTest {
    private String schema;
    private JdbcTemplate admin, jdbc;
    private EntityManagerFactory emf;
    private EntityManager em;
    private TransactionTemplate transaction;
    private HeritageSousDossierService service;
    private ConteneurRepository conteneurs;
    private DocumentRepository documents;
    private RemiseRepository remises;
    private int mereId, premierId, secondId, conteneurId, documentId, typeDocumentId;
    private final LocalDateTime ajout = LocalDateTime.of(2026, 10, 3, 9, 15);
    private final LocalDateTime dateRemise = ajout.minusDays(1);

    @BeforeEach void setup() {
        var base = new DriverManagerDataSource(System.getenv("AUXIMAD_TEST_DATABASE_URL"),
                System.getenv("AUXIMAD_TEST_DATABASE_USER"), System.getenv("AUXIMAD_TEST_DATABASE_PASSWORD"));
        admin = new JdbcTemplate(base);
        schema = "heritage_test_" + UUID.randomUUID().toString().replace("-", "");
        admin.execute("CREATE SCHEMA " + schema);
        var isolated = new DriverManagerDataSource(base.getUrl(), base.getUsername(), base.getPassword());
        Properties properties = new Properties(); properties.setProperty("currentSchema", schema);
        isolated.setConnectionProperties(properties); jdbc = new JdbcTemplate(isolated);
        var factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(isolated); factory.setPackagesToScan("com.auximad.entity");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop", "hibernate.default_schema", schema));
        factory.afterPropertiesSet(); emf = factory.getObject();
        em = SharedEntityManagerCreator.createSharedEntityManager(emf);
        transaction = new TransactionTemplate(new JpaTransactionManager(emf));
        var factoryRepositories = new JpaRepositoryFactory(em);
        conteneurs = factoryRepositories.getRepository(ConteneurRepository.class);
        documents = factoryRepositories.getRepository(DocumentRepository.class);
        remises = factoryRepositories.getRepository(RemiseRepository.class);
        ConteneurService eligibilite = mock(ConteneurService.class);
        when(eligibilite.estEligibleConteneur(any())).thenReturn(true);
        service = new HeritageSousDossierService(conteneurs, documents, remises, eligibilite);
        transaction.executeWithoutResult(status -> fixtures());
    }

    private void fixtures() {
        Agence agence = new Agence(); agence.setCode(20); agence.setNom("Test");
        agence.setAdresse("Adresse test"); agence.setVille("Antananarivo"); agence.setCpClient("101");
        em.persist(agence);
        Role role = new Role(); role.setNom("Test"); em.persist(role);
        Utilisateur utilisateur = new Utilisateur(); utilisateur.setNomUtilisateur("test_heritage");
        utilisateur.setNom("Test"); utilisateur.setPrenom("Heritage"); utilisateur.setEmail("test@example.invalid");
        utilisateur.setMotDePasse("test-hash-unused"); utilisateur.setRole(role); utilisateur.setAgence(agence);
        em.persist(utilisateur);
        Client client = new Client(); client.setNom("Client test"); em.persist(client);
        Dossier mere = dossier("MERE", agence, utilisateur, client);
        Dossier premier = dossier("ENFANT-1", agence, utilisateur, client);
        Dossier second = dossier("ENFANT-2", agence, utilisateur, client);
        mereId = mere.getId(); premierId = premier.getId(); secondId = second.getId();
        Conteneur conteneur = new Conteneur(); conteneur.setNumeroContainer("HERITAGE-001");
        conteneur.setDossier(mere); em.persist(conteneur); conteneurId = conteneur.getId();
        TypeDocument type = new TypeDocument(); type.setNom("Facture test"); em.persist(type); typeDocumentId = type.getId();
        Document doc = new Document(); doc.setDossier(mere); doc.setTypeDocument(type); doc.setNombre(3);
        doc.setCheminFichier("1/facture-test.pdf"); doc.setNomFichier("facture-test.pdf"); doc.setDateAjout(ajout);
        em.persist(doc); documentId = doc.getId();
        Document retire = new Document(); retire.setDossier(mere); retire.setTypeDocument(type);
        retire.setEstActif(false); em.persist(retire);
        Remise remise = new Remise(); remise.setDossier(mere); remise.setTypeDocument(type); remise.setDateRemise(dateRemise);
        em.persist(remise);
    }

    private Dossier dossier(String numero, Agence agence, Utilisateur utilisateur, Client client) {
        Dossier dossier = new Dossier(); dossier.setNumeroDossier(numero); dossier.setAgence(agence);
        dossier.setUtilisateur(utilisateur); dossier.setClient(client); em.persist(dossier); return dossier;
    }

    @AfterEach void cleanup() {
        if (emf != null) emf.close();
        if (admin != null && schema != null && schema.matches("heritage_test_[a-f0-9]{32}")) {
            admin.execute("DROP SCHEMA " + schema + " CASCADE");
        }
    }

    @Test void associationsAndDocumentCopiesArePersistedForBothAgencies() {
        transaction.executeWithoutResult(status -> {
            Dossier mere = em.find(Dossier.class, mereId);
            service.copierRessources(mere, em.find(Dossier.class, premierId));
            service.copierRessources(mere, em.find(Dossier.class, secondId));
        });
        assertEquals(1, count("conteneur")); assertEquals(3, count("dossier_conteneur"));
        assertEquals(4, count("document")); assertEquals(3, count("remise"));
        for (int enfantId : List.of(premierId, secondId)) {
            List<Conteneur> affectes = conteneurs.findByDossierId(enfantId);
            assertEquals(1, affectes.size()); assertEquals(conteneurId, affectes.get(0).getId());
            List<Document> actifs = documents.findByDossierIdAndEstActifTrue(enfantId);
            assertEquals(1, actifs.size()); Document copie = actifs.get(0);
            assertNotEquals(documentId, copie.getId()); assertEquals(typeDocumentId, copie.getTypeDocument().getId());
            assertEquals(3, copie.getNombre()); assertEquals("1/facture-test.pdf", copie.getCheminFichier());
            assertEquals("facture-test.pdf", copie.getNomFichier()); assertEquals(ajout, copie.getDateAjout());
            assertEquals(dateRemise, remises.findByDossierId(enfantId).get(0).getDateRemise());
        }
        assertEquals(mereId, documents.findById(documentId).orElseThrow().getDossier().getId());
        assertEquals(1, conteneurs.findByDossierId(mereId).size());
    }

    @Test void failureRollsBackContainersDocumentsAndRemisesTogether() {
        assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
            service.copierRessources(em.find(Dossier.class, mereId), em.find(Dossier.class, premierId));
            em.flush();
            throw new IllegalStateException("Simulation d'echec apres les ecritures");
        }));
        assertEquals(1, count("conteneur")); assertEquals(1, count("dossier_conteneur"));
        assertEquals(2, count("document")); assertEquals(1, count("remise"));
        assertTrue(conteneurs.findByDossierId(premierId).isEmpty());
        assertTrue(documents.findByDossierIdAndEstActifTrue(premierId).isEmpty());
        assertTrue(remises.findByDossierId(premierId).isEmpty());
    }

    private int count(String table) { return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class); }
}
