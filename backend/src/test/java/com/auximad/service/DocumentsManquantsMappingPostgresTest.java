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

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/** Verifie le mapping du resultat utilise quand un mouvement manque de documents. */
@EnabledIfEnvironmentVariable(named = "AUXIMAD_TEST_DATABASE_URL", matches = ".+")
class DocumentsManquantsMappingPostgresTest {
    private String schema;
    private JdbcTemplate admin;
    private EntityManagerFactory emf;
    private EntityManager em;
    private TransactionTemplate transaction;
    private DocumentObligatoireMvtRepository obligations;
    private DocumentService service;
    private int dossierId, sansOtId, operationId, natureId;
    private final Map<String, Integer> types = new HashMap<>();

    @BeforeEach void setup() {
        var base = new DriverManagerDataSource(System.getenv("AUXIMAD_TEST_DATABASE_URL"),
                System.getenv("AUXIMAD_TEST_DATABASE_USER"), System.getenv("AUXIMAD_TEST_DATABASE_PASSWORD"));
        admin = new JdbcTemplate(base);
        schema = "documents_mapping_test_" + UUID.randomUUID().toString().replace("-", "");
        admin.execute("CREATE SCHEMA " + schema);
        var isolated = new DriverManagerDataSource(base.getUrl(), base.getUsername(), base.getPassword());
        Properties properties = new Properties(); properties.setProperty("currentSchema", schema);
        isolated.setConnectionProperties(properties);
        var factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(isolated); factory.setPackagesToScan("com.auximad.entity");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop", "hibernate.default_schema", schema));
        factory.afterPropertiesSet(); emf = factory.getObject();
        em = SharedEntityManagerCreator.createSharedEntityManager(emf);
        transaction = new TransactionTemplate(new JpaTransactionManager(emf));
        var factoryRepositories = new JpaRepositoryFactory(em);
        obligations = factoryRepositories.getRepository(DocumentObligatoireMvtRepository.class);
        service = new DocumentService(factoryRepositories.getRepository(DocumentRepository.class),
                factoryRepositories.getRepository(TypeDocumentRepository.class),
                factoryRepositories.getRepository(DossierRepository.class),
                factoryRepositories.getRepository(RemiseRepository.class), obligations, mock(SuppressionService.class));
        transaction.executeWithoutResult(status -> fixtures());
    }

    private void fixtures() {
        Agence agence = new Agence(); agence.setNom("Test mapping"); agence.setCode(20);
        agence.setAdresse("Adresse de test"); agence.setVille("Antananarivo"); agence.setCpClient("101"); em.persist(agence);
        Role role = new Role(); role.setNom("Test mapping"); em.persist(role);
        Utilisateur utilisateur = new Utilisateur(); utilisateur.setNomUtilisateur("test_mapping");
        utilisateur.setNom("Test"); utilisateur.setPrenom("Mapping"); utilisateur.setEmail("test-mapping@example.invalid");
        utilisateur.setMotDePasse("test-unused-hash"); utilisateur.setRole(role); utilisateur.setAgence(agence); em.persist(utilisateur);
        Client client = new Client(); client.setNom("Client test mapping"); em.persist(client);
        NatureMarchandise nature = new NatureMarchandise(); nature.setNom("Textile mapping"); em.persist(nature); natureId = nature.getId();
        NatureMarchandise autre = new NatureMarchandise(); autre.setNom("Autre mapping"); em.persist(autre);
        OrdreTransit ot = new OrdreTransit(); ot.setNatureMarchandise(nature); em.persist(ot);
        Dossier dossier = dossier("MAPPING-001", agence, utilisateur, client); dossier.setOrdreTransit(ot);
        dossierId = dossier.getId(); sansOtId = dossier("MAPPING-SANS-OT", agence, utilisateur, client).getId();
        OperationDossier operation = new OperationDossier(); operation.setDescription("Declaration mapping"); em.persist(operation); operationId = operation.getId();
        type("A-general-manquant", null, operation);
        type("B-textile-manquant", nature, operation);
        TypeDocument remis = type("C-general-remis", null, operation);
        type("D-autre-nature", autre, operation);
        TypeDocument inactif = type("E-general-retire", null, operation);
        Document actif = new Document(); actif.setDossier(dossier); actif.setTypeDocument(remis); em.persist(actif);
        Document retire = new Document(); retire.setDossier(dossier); retire.setTypeDocument(inactif); retire.setEstActif(false); em.persist(retire);
    }

    private Dossier dossier(String numero, Agence agence, Utilisateur utilisateur, Client client) {
        Dossier dossier = new Dossier(); dossier.setNumeroDossier(numero); dossier.setAgence(agence);
        dossier.setUtilisateur(utilisateur); dossier.setClient(client); em.persist(dossier); return dossier;
    }

    private TypeDocument type(String nom, NatureMarchandise nature, OperationDossier operation) {
        TypeDocument type = new TypeDocument(); type.setNom(nom); type.setNatureMarchandise(nature); em.persist(type);
        types.put(nom, type.getId());
        DocumentObligatoireMvt obligation = new DocumentObligatoireMvt(); obligation.setOperation(operation); obligation.setTypeDocument(type); em.persist(obligation);
        return type;
    }

    @AfterEach void cleanup() {
        if (emf != null) emf.close();
        if (admin != null && schema != null && schema.matches("documents_mapping_test_[a-f0-9]{32}"))
            admin.execute("DROP SCHEMA " + schema + " CASCADE");
    }

    @Test void missingTypesAreMappedAsEntitiesAndFilteredByNatureAndActiveDocuments() {
        List<TypeDocument> manquants = service.getDocsManquantsPourOperation(dossierId, operationId);
        assertEquals(List.of("A-general-manquant", "B-textile-manquant", "E-general-retire"),
                manquants.stream().map(TypeDocument::getNom).toList());
        assertEquals(types.get("B-textile-manquant"), manquants.get(1).getId());
        assertEquals(natureId, manquants.get(1).getNatureMarchandise().getId());
        assertEquals(manquants.size(), obligations.countManquants(dossierId, operationId));
    }

    @Test void dossierWithoutOtKeepsGeneralRequirementsAndAnUnknownOperationIsEmpty() {
        List<TypeDocument> manquants = service.getDocsManquantsPourOperation(sansOtId, operationId);
        assertEquals(List.of("A-general-manquant", "C-general-remis", "E-general-retire"),
                manquants.stream().map(TypeDocument::getNom).toList());
        assertEquals(manquants.size(), obligations.countManquants(sansOtId, operationId));
        assertTrue(service.getDocsManquantsPourOperation(dossierId, -1).isEmpty());
    }
}
