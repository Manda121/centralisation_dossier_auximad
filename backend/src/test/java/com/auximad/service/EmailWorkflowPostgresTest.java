package com.auximad.service;

import com.auximad.dto.AdresseEmailAgenceRequest;
import com.auximad.entity.*;
import com.auximad.repository.*;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Vérifie migration, mapping JPA, transaction de la file et historique dans un schéma isolé. */
@EnabledIfEnvironmentVariable(named = "AUXIMAD_TEST_DATABASE_URL", matches = ".+")
class EmailWorkflowPostgresTest {
    private String schema;
    private JdbcTemplate admin, jdbc;
    private EntityManagerFactory emf;
    private TransactionTemplate transaction;
    private ConfigurationEmailService configuration;
    private NotificationInteragenceService notifications;
    private EnvoiMailService envoi;
    private JavaMailSender sender;
    private HistoriqueMailRepository historique;
    private DemandeInteragence demande;

    @BeforeEach void setup() throws Exception {
        var base = new DriverManagerDataSource(System.getenv("AUXIMAD_TEST_DATABASE_URL"),
                System.getenv("AUXIMAD_TEST_DATABASE_USER"), System.getenv("AUXIMAD_TEST_DATABASE_PASSWORD"));
        admin = new JdbcTemplate(base);
        schema = "email_test_" + UUID.randomUUID().toString().replace("-", "");
        admin.execute("CREATE SCHEMA " + schema);
        var dataSource = new DriverManagerDataSource(base.getUrl(), base.getUsername(), base.getPassword());
        Properties properties = new Properties(); properties.setProperty("currentSchema", schema);
        dataSource.setConnectionProperties(properties); jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
            CREATE TABLE agence(id int PRIMARY KEY, code int, nom varchar(100), adresse varchar(200),
                ville varchar(100), cp_client varchar(10), date_creation timestamptz);
            CREATE TABLE demande_interagence(id int PRIMARY KEY);
            INSERT INTO agence VALUES(1, 1, 'Antananarivo', '', '', '', NOW()),
                (2, 2, 'Toamasina', '', '', '', NOW());
            """);
        String migration = Files.readString(Path.of("bdd/migrations/20261102_emails_interagences.sql"));
        jdbc.execute(migration.replace("BEGIN;", "").replace("COMMIT;", ""));
        // La migration peut être rejouée sans affecter les données existantes.
        jdbc.execute(migration.replace("BEGIN;", "").replace("COMMIT;", ""));
        var factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource); factory.setPackagesToScan("com.auximad.entity");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "none"));
        factory.afterPropertiesSet(); emf = factory.getObject();
        transaction = new TransactionTemplate(new JpaTransactionManager(emf));
        var repositories = new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(emf));
        historique = repositories.getRepository(HistoriqueMailRepository.class);
        var adresses = repositories.getRepository(AdresseEmailAgenceRepository.class);
        var agences = repositories.getRepository(AgenceRepository.class);
        configuration = new ConfigurationEmailService(adresses, agences);
        notifications = new NotificationInteragenceService(adresses, historique);
        var demandes = mock(DemandeInteragenceRepository.class);
        Agence source = new Agence(); source.setId(1); source.setNom("Antananarivo");
        Agence destination = new Agence(); destination.setId(2); destination.setNom("Toamasina");
        Dossier dossier = new Dossier(); dossier.setNumeroDossier("DOS-TEST");
        demande = new DemandeInteragence(); demande.setId(42); demande.setDossier(dossier);
        demande.setAgenceDemandeuse(source); demande.setAgenceDestinataire(destination);
        when(demandes.findById(42)).thenReturn(Optional.of(demande));
        sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenAnswer(call -> new MimeMessage(Session.getInstance(new Properties())));
        @SuppressWarnings("unchecked") ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender); when(provider.getObject()).thenReturn(sender);
        envoi = new EnvoiMailService(historique, provider, "notification@auximad.mg", "smtp.test", demandes);
        transaction.executeWithoutResult(status -> {
            configuration.enregistrer(null, new AdresseEmailAgenceRequest(2, "SERVICE@auximad.mg", true));
            configuration.enregistrer(null, new AdresseEmailAgenceRequest(2, "inactif@auximad.mg", false));
        });
    }

    @AfterEach void cleanup() {
        if (emf != null) emf.close();
        if (admin != null && schema != null && schema.matches("email_test_[a-f0-9]{32}")) {
            admin.execute("DROP SCHEMA " + schema + " CASCADE");
        }
    }

    @Test void rollbackRemovesRequestAndAllQueuedMail() {
        transaction.executeWithoutResult(status -> {
            jdbc.update("INSERT INTO demande_interagence VALUES(42)");
            notifications.preparer(demande);
            status.setRollbackOnly();
        });
        assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM demande_interagence", Long.class));
        assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM historique_mail", Long.class));
        verify(sender, never()).send(any(MimeMessage.class));
    }

    @Test void failedDeliveryAndRetryKeepSeparateDurableHistory() {
        transaction.executeWithoutResult(status -> {
            jdbc.update("INSERT INTO demande_interagence VALUES(42)");
            notifications.preparer(demande);
        });
        assertEquals(1L, jdbc.queryForObject("SELECT count(*) FROM historique_mail", Long.class));
        transaction.executeWithoutResult(status -> {
            assertEquals(2, configuration.lister().size());
            assertEquals(1L, historique.rechercher(null, null, PageRequest.of(0, 15)).getTotalElements());
            assertEquals(1L, historique.rechercher(2, HistoriqueMail.Statut.EN_ATTENTE,
                    PageRequest.of(0, 15)).getTotalElements());
            assertEquals(0L, historique.rechercher(1, null, PageRequest.of(0, 15)).getTotalElements());
        });
        Long original = jdbc.queryForObject("SELECT id FROM historique_mail", Long.class);
        doThrow(new MailSendException("SMTP unavailable")).when(sender).send(any(MimeMessage.class));
        transaction.executeWithoutResult(status -> envoi.traiter(original));
        assertEquals("ECHEC", jdbc.queryForObject("SELECT statut FROM historique_mail WHERE id=?", String.class, original));
        transaction.executeWithoutResult(status -> notifications.relancer(original));
        Long relance = jdbc.queryForObject("SELECT id FROM historique_mail WHERE id_mail_origine=?", Long.class, original);
        doNothing().when(sender).send(any(MimeMessage.class));
        transaction.executeWithoutResult(status -> envoi.traiter(relance));
        assertEquals("ENVOYE", jdbc.queryForObject("SELECT statut FROM historique_mail WHERE id=?", String.class, relance));
        assertEquals("ECHEC", jdbc.queryForObject("SELECT statut FROM historique_mail WHERE id=?", String.class, original));
        assertEquals("service@auximad.mg", jdbc.queryForObject("SELECT destinataire FROM historique_mail WHERE id=?", String.class, relance));
        transaction.executeWithoutResult(status -> envoi.traiter(relance));
        verify(sender, times(2)).send(any(MimeMessage.class));
    }
}
