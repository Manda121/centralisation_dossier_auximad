package com.auximad.service;

import com.auximad.entity.*;
import com.auximad.repository.*;
import com.auximad.dto.MvtDossierRequest;
import io.jsonwebtoken.Jwts;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/** Verifie la famille de dossiers avec de vraies associations JPA dans un schema jetable. */
@EnabledIfEnvironmentVariable(named = "AUXIMAD_TEST_DATABASE_URL", matches = ".+")
class SynchronisationMouvementsPostgresTest {
    private String schema;
    private JdbcTemplate admin, jdbc;
    private EntityManagerFactory emf;
    private EntityManager em;
    private TransactionTemplate tx;
    private SynchronisationMouvementsService service;
    private DossierWorkflowService workflow;
    private MvtDossierService saisie;
    private MvtDossierRepository mouvements;
    private DocumentRepository documents;
    private DocumentMvtRepository pieces;
    private int mereId, enfantId, soeurId, seulId, utilisateurId, premiereId, secondeId, preparationId, traitementId;
    private int ouvertId, avanceId, mouvementInitialId, fichierInitialId;
    private final LocalDateTime premiereDate = LocalDateTime.of(2026, 10, 3, 9, 15);
    private final LocalDateTime transitionDate = premiereDate.plusHours(2);

    @BeforeEach void setup() {
        var base = new DriverManagerDataSource(System.getenv("AUXIMAD_TEST_DATABASE_URL"),
                System.getenv("AUXIMAD_TEST_DATABASE_USER"), System.getenv("AUXIMAD_TEST_DATABASE_PASSWORD"));
        admin = new JdbcTemplate(base);
        schema = "sync_mvt_test_" + UUID.randomUUID().toString().replace("-", "");
        admin.execute("CREATE SCHEMA " + schema);
        var isolated = new DriverManagerDataSource(base.getUrl(), base.getUsername(), base.getPassword());
        Properties props = new Properties(); props.setProperty("currentSchema", schema);
        isolated.setConnectionProperties(props); jdbc = new JdbcTemplate(isolated);
        var factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(isolated); factory.setPackagesToScan("com.auximad.entity");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop", "hibernate.default_schema", schema));
        factory.afterPropertiesSet(); emf = factory.getObject();
        jdbc.execute("CREATE UNIQUE INDEX mvt_partage_actif_test ON mvt_dossier(id_dossier, cle_partage) "
                + "WHERE cle_partage IS NOT NULL AND est_annule = FALSE");
        em = SharedEntityManagerCreator.createSharedEntityManager(emf);
        tx = new TransactionTemplate(new JpaTransactionManager(emf));
        var repositories = new JpaRepositoryFactory(em);
        mouvements = repositories.getRepository(MvtDossierRepository.class);
        documents = repositories.getRepository(DocumentRepository.class);
        pieces = repositories.getRepository(DocumentMvtRepository.class);
        service = new SynchronisationMouvementsService(
                repositories.getRepository(DossierRepository.class), mouvements,
                repositories.getRepository(MvtAnnuleeRepository.class), pieces, documents,
                repositories.getRepository(UtilisateurRepository.class));
        workflow = new DossierWorkflowService(repositories.getRepository(DossierRepository.class),
                repositories.getRepository(PhaseDossierRepository.class), repositories.getRepository(StatutDossierRepository.class),
                repositories.getRepository(OperationDossierRepository.class), mouvements,
                repositories.getRepository(MvtAnnuleeRepository.class), repositories.getRepository(TypeDocumentRepository.class),
                documents, repositories.getRepository(UtilisateurRepository.class), service);
        DocumentService documentService = new DocumentService(documents, repositories.getRepository(TypeDocumentRepository.class),
                repositories.getRepository(DossierRepository.class), repositories.getRepository(RemiseRepository.class),
                repositories.getRepository(DocumentObligatoireMvtRepository.class), mock(SuppressionService.class));
        saisie = new MvtDossierService(mouvements, repositories.getRepository(DossierRepository.class),
                repositories.getRepository(OperationDossierRepository.class), repositories.getRepository(UtilisateurRepository.class),
                repositories.getRepository(DeclarationRepository.class), documents, pieces, documentService, workflow, service);
        tx.executeWithoutResult(status -> fixtures());
    }

    private void fixtures() {
        Agence agence = new Agence(); agence.setCode(20); agence.setNom("TNR");
        agence.setAdresse("Adresse test"); agence.setVille("Antananarivo"); agence.setCpClient("101"); em.persist(agence);
        Role role = new Role(); role.setNom("Agent test"); em.persist(role);
        Utilisateur auteur = new Utilisateur(); auteur.setNomUtilisateur("sync_test"); auteur.setAgence(agence);
        auteur.setNom("Test"); auteur.setPrenom("Synchro"); auteur.setEmail("sync@example.invalid");
        auteur.setMotDePasse("hash-unused"); auteur.setRole(role);
        em.persist(auteur); utilisateurId = auteur.getId();
        Client client = new Client(); client.setNom("Client test"); em.persist(client);
        ModeTransport mode = new ModeTransport(); mode.setNom("Maritime"); em.persist(mode);
        TypeProduction production = new TypeProduction(); production.setNom("import"); production.setCode(1); production.setModeTransport(mode);
        em.persist(production);
        PhaseDossier preparation = new PhaseDossier(); preparation.setNom("PREPARATION"); em.persist(preparation);
        PhaseDossier traitement = new PhaseDossier(); traitement.setNom("TRAITEMENT"); em.persist(traitement);
        preparationId = preparation.getId(); traitementId = traitement.getId();
        StatutDossier ouvert = new StatutDossier(); ouvert.setNom("Ouvert"); em.persist(ouvert);
        StatutDossier avance = new StatutDossier(); avance.setNom("Declaration etablie"); em.persist(avance);
        ouvertId = ouvert.getId(); avanceId = avance.getId();
        OperationDossier premiere = operation("Avis client", 1, production, preparation, ouvert);
        OperationDossier seconde = operation("Declaration", 2, production, traitement, avance);
        premiereId = premiere.getId(); secondeId = seconde.getId();
        Dossier mere = dossier("MERE", agence, auteur, client, production, traitement, avance);
        Dossier enfant = dossier("ENFANT", agence, auteur, client, production, preparation, ouvert);
        Dossier soeur = dossier("SOEUR", agence, auteur, client, production, preparation, ouvert);
        Dossier seul = dossier("SANS-LIEN", agence, auteur, client, production, preparation, ouvert);
        mereId = mere.getId(); enfantId = enfant.getId(); soeurId = soeur.getId(); seulId = seul.getId();
        lien(mere, enfant); lien(mere, soeur);
        MvtDossier premier = mouvement(mere, premiere, premiereDate, preparation, ouvert);
        premier.setAnciennePhase(preparation); premier.setAncienStatut(ouvert); premier.setCommentaire("Avis initial");
        mouvementInitialId = premier.getId();
        MvtDossier transition = mouvement(mere, null, transitionDate, traitement, avance);
        transition.setOrigine("AJUSTEMENT_PHASE"); transition.setAnciennePhase(preparation);
        transition.setAncienStatut(ouvert); transition.setCommentaire("Phase ajustee");
        TypeDocument type = new TypeDocument(); type.setNom("Piece mouvement"); em.persist(type);
        Document fichier = new Document(); fichier.setDossier(mere); fichier.setTypeDocument(type);
        fichier.setNomFichier("avis.pdf"); fichier.setCheminFichier("dossier-1/avis.pdf"); fichier.setDateAjout(premiereDate);
        em.persist(fichier); fichierInitialId = fichier.getId();
        DocumentMvt piece = new DocumentMvt(); piece.setMvtDossier(premier); piece.setDocument(fichier); em.persist(piece);
    }

    private OperationDossier operation(String description, int niveau, TypeProduction production, PhaseDossier phase, StatutDossier statut) {
        OperationDossier op = new OperationDossier(); op.setDescription(description); op.setNiveauOperation(niveau);
        op.setTypeProduction(production); op.setPhaseDossier(phase); op.setStatutDossier(statut); em.persist(op); return op;
    }
    private Dossier dossier(String numero, Agence agence, Utilisateur auteur, Client client, TypeProduction production,
                            PhaseDossier phase, StatutDossier statut) {
        Dossier d = new Dossier(); d.setNumeroDossier(numero); d.setAgence(agence); d.setUtilisateur(auteur);
        d.setClient(client); d.setTypeProduction(production); d.setPhaseDossier(phase); d.setStatutDossier(statut);
        em.persist(d); return d;
    }
    private void lien(Dossier mere, Dossier enfant) {
        DossierInteragence lien = new DossierInteragence(); lien.setDossierMere(mere); lien.setSousDossier(enfant); em.persist(lien);
    }
    private MvtDossier mouvement(Dossier dossier, OperationDossier operation, LocalDateTime date, PhaseDossier phase, StatutDossier statut) {
        MvtDossier m = new MvtDossier(); m.setDossier(dossier); m.setOperation(operation); m.setDateMvt(date);
        m.setUtilisateur(em.find(Utilisateur.class, utilisateurId)); m.setNouvellePhase(phase); m.setNouveauStatut(statut);
        em.persist(m); return m;
    }
    private void initialiserFamille() { tx.executeWithoutResult(status -> service.alignerDepuisMere(enfantId)); }

    @AfterEach void cleanup() {
        SecurityContextHolder.clearContext();
        if (emf != null) emf.close();
        if (admin != null && schema != null && schema.matches("sync_mvt_test_[a-f0-9]{32}")) {
            admin.execute("DROP SCHEMA " + schema + " CASCADE");
        }
    }

    @Test void bootstrapCopiesOperationsTransitionsDatesAndAttachmentsWithoutDuplication() {
        initialiserFamille();
        initialiserFamille();
        assertEquals(6, count("mvt_dossier")); assertEquals(3, count("document_mvt"));
        assertEquals(3, count("document"));
        List<MvtDossier> source = mouvements.findActifsByDossierIdOrderByIdAsc(mereId);
        assertNotNull(source.get(0).getClePartage()); assertNotNull(source.get(1).getClePartage());
        for (int id : List.of(enfantId, soeurId)) {
            List<MvtDossier> actifs = mouvements.findActifsByDossierIdOrderByIdAsc(id);
            assertEquals(2, actifs.size());
            assertEquals(premiereId, actifs.get(0).getOperation().getId());
            assertEquals(premiereDate, actifs.get(0).getDateMvt());
            assertEquals(utilisateurId, actifs.get(0).getUtilisateur().getId());
            assertEquals("Avis initial", actifs.get(0).getCommentaire());
            assertEquals(source.get(0).getClePartage(), actifs.get(0).getClePartage());
            assertNull(actifs.get(1).getOperation()); assertEquals(transitionDate, actifs.get(1).getDateMvt());
            Document copie = pieces.findFirstByMvtDossierIdOrderByIdDesc(actifs.get(0).getId()).orElseThrow().getDocument();
            assertNotEquals(fichierInitialId, copie.getId()); assertEquals(id, copie.getDossier().getId());
            assertEquals("dossier-1/avis.pdf", copie.getCheminFichier()); assertEquals("avis.pdf", copie.getNomFichier());
            assertEtat(id, traitementId, avanceId);
        }
    }

    @Test void movementCreatedFromChildAdvancesParentAndOtherChild() {
        initialiserFamille();
        tx.executeWithoutResult(status -> {
            service.verrouillerFamille(enfantId);
            Dossier enfant = em.find(Dossier.class, enfantId);
            mouvement(enfant, em.find(OperationDossier.class, secondeId), transitionDate.plusHours(3),
                    em.find(PhaseDossier.class, traitementId), em.find(StatutDossier.class, avanceId));
            service.synchroniserDepuis(enfant);
        });
        for (int id : List.of(mereId, enfantId, soeurId)) {
            List<MvtDossier> actifs = mouvements.findActifsByDossierIdOrderByIdAsc(id);
            assertEquals(3, actifs.size()); assertEquals(secondeId, actifs.get(2).getOperation().getId());
            assertEquals(transitionDate.plusHours(3), actifs.get(2).getDateMvt());
        }
    }

    @Test void cancellationFromChildPreservesHistoryAndCancelsTheSharedOperationEverywhere() {
        initialiserFamille();
        tx.executeWithoutResult(status -> {
            Dossier enfant = em.find(Dossier.class, enfantId);
            mouvement(enfant, em.find(OperationDossier.class, secondeId), transitionDate.plusHours(3),
                    em.find(PhaseDossier.class, traitementId), em.find(StatutDossier.class, avanceId));
            service.synchroniserDepuis(enfant);
        });
        tx.executeWithoutResult(status -> {
            service.verrouillerFamille(enfantId);
            Dossier enfant = em.find(Dossier.class, enfantId);
            MvtDossier dernier = mouvements.findActifsByDossierIdOrderByIdAsc(enfantId).get(2);
            MvtAnnulee annulation = new MvtAnnulee(); annulation.setMouvement(dernier);
            annulation.setUtilisateurAnnulation(em.find(Utilisateur.class, utilisateurId));
            annulation.setMotifAnnulation("Erreur de saisie"); em.persist(annulation);
            dernier.setEstAnnule(true); dernier.setAnnulation(annulation);
            service.synchroniserDepuis(enfant);
        });
        int historique = count("mvt_dossier"); int annulations = count("mvt_annulee");
        assertTrue(historique >= 9);
        assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM mvt_dossier WHERE id_operation=? AND est_annule", Integer.class, secondeId));
        tx.executeWithoutResult(status -> service.synchroniserDepuis(em.find(Dossier.class, enfantId)));
        assertEquals(historique, count("mvt_dossier")); assertEquals(annulations, count("mvt_annulee"));
        for (int id : List.of(mereId, enfantId, soeurId)) {
            List<MvtDossier> actifs = mouvements.findActifsByDossierIdOrderByIdAsc(id);
            assertEquals(2, actifs.size());
            assertTrue(actifs.stream().noneMatch(m -> m.getOperation() != null && m.getOperation().getId() == secondeId));
        }
    }

    @Test void failureAfterPhysicalWritesRollsBackEveryFamilyMember() {
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status -> {
            service.alignerDepuisMere(enfantId); em.flush();
            throw new IllegalStateException("Echec simule apres ecriture");
        }));
        assertEquals(2, count("mvt_dossier")); assertEquals(1, count("document")); assertEquals(1, count("document_mvt"));
        assertTrue(mouvements.findActifsByDossierIdOrderByIdAsc(enfantId).isEmpty());
        assertTrue(mouvements.findActifsByDossierIdOrderByIdAsc(soeurId).isEmpty());
        assertNull(mouvements.findById(mouvementInitialId).orElseThrow().getClePartage());
    }

    @Test void nestedDescendantsResolveTheSameFamilyFromTheDeepestChild() {
        tx.executeWithoutResult(status -> {
            DossierInteragence second = em.createQuery("select l from DossierInteragence l where l.sousDossier.id=:id", DossierInteragence.class)
                    .setParameter("id", soeurId).getSingleResult();
            second.setDossierMere(em.find(Dossier.class, enfantId));
        });
        tx.executeWithoutResult(status -> {
            assertEquals(List.of(mereId, enfantId, soeurId), service.verrouillerFamille(soeurId).stream().map(Dossier::getId).toList());
            service.alignerDepuisMere(soeurId);
        });
        assertEquals(6, count("mvt_dossier"));
        assertEquals(2, mouvements.findActifsByDossierIdOrderByIdAsc(soeurId).size());
    }

    @Test void standaloneDossierDoesNotChangeAnotherFamilysProgress() {
        tx.executeWithoutResult(status -> {
            assertEquals(List.of(seulId), service.verrouillerFamille(seulId).stream().map(Dossier::getId).toList());
            service.alignerDepuisMere(seulId);
            service.synchroniserDepuis(em.find(Dossier.class, seulId));
        });
        assertEquals(2, count("mvt_dossier"));
        assertTrue(mouvements.findActifsByDossierIdOrderByIdAsc(seulId).isEmpty());
        assertTrue(mouvements.findActifsByDossierIdOrderByIdAsc(enfantId).isEmpty());
    }

    @Test void realMovementAndCancellationServicesKeepTheWholeFamilyInSync() {
        initialiserFamille(); authentifier();
        MvtDossierRequest request = new MvtDossierRequest(); request.setDossierId(enfantId); request.setOperationId(secondeId);
        request.setDateMvt(transitionDate.plusHours(3)); request.setCommentaire("Validation depuis agence destinataire");
        Integer mvtId = tx.execute(status -> creer(request).getId());
        assertNotNull(mvtId);
        for (int id : List.of(mereId, enfantId, soeurId)) {
            List<MvtDossier> actifs = mouvements.findActifsByDossierIdOrderByIdAsc(id);
            assertEquals(3, actifs.size()); assertEquals(secondeId, actifs.get(2).getOperation().getId());
            assertEtat(id, traitementId, avanceId);
        }
        int total = count("mvt_dossier");
        IllegalArgumentException erreur = assertThrows(IllegalArgumentException.class,
                () -> tx.executeWithoutResult(status -> creer(request)));
        assertTrue(erreur.getMessage().contains("déjà effectuée"));
        assertEquals(total, count("mvt_dossier"));

        tx.executeWithoutResult(status -> workflow.annulerMouvement(mvtId, "Erreur de saisie", true));
        for (int id : List.of(mereId, enfantId, soeurId)) {
            List<MvtDossier> actifs = mouvements.findActifsByDossierIdOrderByIdAsc(id);
            assertEquals(2, actifs.size());
            assertTrue(actifs.stream().noneMatch(m -> m.getOperation() != null && m.getOperation().getId() == secondeId));
            assertEtat(id, traitementId, avanceId);
        }
        assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM mvt_dossier WHERE id_operation=? AND est_annule", Integer.class, secondeId));
    }

    @Test void realPhaseAndStatusChangesFromDifferentAgenciesShareTheirProgress() {
        initialiserFamille(); authentifier();
        tx.executeWithoutResult(status -> workflow.changerPhase(enfantId, preparationId));
        for (int id : List.of(mereId, enfantId, soeurId)) assertEtat(id, preparationId, ouvertId);

        tx.executeWithoutResult(status -> workflow.changerStatut(soeurId, avanceId));
        for (int id : List.of(mereId, enfantId, soeurId)) {
            assertEtat(id, traitementId, avanceId);
            assertTrue(mouvements.findActifsByDossierIdOrderByIdAsc(id).stream()
                    .anyMatch(m -> m.getOperation() != null && m.getOperation().getId() == secondeId));
        }
    }

    @Test void simultaneousValidationFromParentAndChildAcceptsTheOperationOnlyOnce() throws Exception {
        initialiserFamille();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch prets = new CountDownLatch(2);
        CountDownLatch depart = new CountDownLatch(1);
        try {
            List<Future<String>> resultats = new ArrayList<>();
            for (int id : List.of(mereId, enfantId)) {
                resultats.add(executor.submit(() -> {
                    authentifier(); prets.countDown();
                    try {
                        if (!depart.await(10, TimeUnit.SECONDS)) throw new AssertionError("Depart concurrent non recu");
                        MvtDossierRequest request = new MvtDossierRequest(); request.setDossierId(id); request.setOperationId(secondeId);
                        request.setDateMvt(transitionDate.plusHours(4));
                        try {
                            tx.executeWithoutResult(status -> creer(request));
                            return "ACCEPTE";
                        } catch (IllegalArgumentException erreur) {
                            assertTrue(erreur.getMessage().contains("déjà effectuée"), erreur.getMessage());
                            return "DEJA_EFFECTUE";
                        }
                    } finally { SecurityContextHolder.clearContext(); }
                }));
            }
            assertTrue(prets.await(5, TimeUnit.SECONDS)); depart.countDown();
            List<String> etats = new ArrayList<>();
            for (Future<String> resultat : resultats) etats.add(resultat.get(15, TimeUnit.SECONDS));
            assertEquals(1, etats.stream().filter("ACCEPTE"::equals).count());
            assertEquals(1, etats.stream().filter("DEJA_EFFECTUE"::equals).count());
            assertEquals(9, count("mvt_dossier"));
            for (int id : List.of(mereId, enfantId, soeurId)) {
                assertEquals(1, mouvements.findActifsByDossierIdOrderByIdAsc(id).stream()
                        .filter(m -> m.getOperation() != null && m.getOperation().getId() == secondeId).count());
                assertEtat(id, traitementId, avanceId);
            }
        } finally {
            depart.countDown(); executor.shutdownNow();
            assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS));
        }
    }

    @Test void startupAlignmentRepairsExistingChildrenAndCanBeRepeatedWithoutDuplicates() {
        var repositories = new JpaRepositoryFactory(em);
        DossierInteragenceRepository liens = repositories.getRepository(DossierInteragenceRepository.class);
        assertEquals(List.of(mereId), liens.findDossierMereIds());
        InitialisationAvancementInteragence initialisation = new InitialisationAvancementInteragence(
                liens, repositories.getRepository(DossierRepository.class), service);
        assertTrue(mouvements.findActifsByDossierIdOrderByIdAsc(enfantId).isEmpty());
        assertTrue(mouvements.findActifsByDossierIdOrderByIdAsc(soeurId).isEmpty());

        tx.executeWithoutResult(status -> initialisation.reprendreFamillesExistantes());
        assertEquals(6, count("mvt_dossier"));
        List<MvtDossier> source = mouvements.findActifsByDossierIdOrderByIdAsc(mereId);
        List<String> cles = source.stream().map(MvtDossier::getClePartage).toList();
        assertEquals(2, new HashSet<>(cles).size()); assertTrue(cles.stream().allMatch(Objects::nonNull));
        for (int id : List.of(enfantId, soeurId)) {
            List<MvtDossier> copies = mouvements.findActifsByDossierIdOrderByIdAsc(id);
            assertEquals(cles, copies.stream().map(MvtDossier::getClePartage).toList());
            assertEquals(premiereId, copies.get(0).getOperation().getId()); assertNull(copies.get(1).getOperation());
            assertEtat(id, traitementId, avanceId);
        }
        List<Integer> ids = jdbc.queryForList("SELECT id FROM mvt_dossier ORDER BY id", Integer.class);
        tx.executeWithoutResult(status -> initialisation.reprendreFamillesExistantes());
        assertEquals(ids, jdbc.queryForList("SELECT id FROM mvt_dossier ORDER BY id", Integer.class));
        assertEquals(3, count("document_mvt")); assertEquals(0, count("mvt_annulee"));
    }

    private MvtDossier creer(MvtDossierRequest request) {
        try { return saisie.creerMouvement(request, null); }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }
    private void authentifier() {
        var auth = new UsernamePasswordAuthenticationToken("sync_test", null);
        auth.setDetails(Jwts.claims().add("userId", utilisateurId).add("niveauAutorisation", 10000).build());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private int count(String table) { return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class); }
    private void assertEtat(int dossierId, int phaseId, int statutId) {
        tx.executeWithoutResult(status -> {
            Dossier dossier = em.find(Dossier.class, dossierId);
            assertEquals(phaseId, dossier.getPhaseDossier().getId()); assertEquals(statutId, dossier.getStatutDossier().getId());
        });
    }
}
