import com.auximad.entity.Dossier;
import com.auximad.repository.*;
import com.auximad.service.SynchronisationMouvementsService;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.file.*;
import java.util.*;

/** Reprise ponctuelle des familles existantes sans serveur, ordonnanceur ou SMTP. */
public class AlignerAvancement {
    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !args[0].equals("--appliquer")) throw new IllegalArgumentException("--appliquer requis");
        Properties config = new Properties();
        try (var reader = Files.newBufferedReader(Path.of("src/main/resources/application.properties"))) {
            config.load(reader);
        }
        var datasource = new DriverManagerDataSource(config.getProperty("spring.datasource.url"),
                config.getProperty("spring.datasource.username"), config.getProperty("spring.datasource.password"));
        var jdbc = new JdbcTemplate(datasource);
        var factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(datasource);
        factory.setPackagesToScan("com.auximad.entity");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "none"));
        factory.afterPropertiesSet();
        try (var emf = factory.getObject()) {
            var em = SharedEntityManagerCreator.createSharedEntityManager(emf);
            var repos = new JpaRepositoryFactory(em);
            var dossiers = repos.getRepository(DossierRepository.class);
            var mouvements = repos.getRepository(MvtDossierRepository.class);
            var service = new SynchronisationMouvementsService(dossiers, mouvements,
                    repos.getRepository(MvtAnnuleeRepository.class), repos.getRepository(DocumentMvtRepository.class),
                    repos.getRepository(DocumentRepository.class), repos.getRepository(UtilisateurRepository.class));
            var tx = new TransactionTemplate(new JpaTransactionManager(emf));
            tx.executeWithoutResult(status -> {
                Set<Integer> traites = new HashSet<>();
                for (Integer id : jdbc.queryForList("SELECT DISTINCT id_dossier_mere FROM dossier_interagence ORDER BY 1", Integer.class)) {
                    List<Dossier> famille = service.verrouillerFamille(id);
                    if (!traites.add(famille.get(0).getId())) continue;
                    service.synchroniserDepuis(famille.get(0));
                    mouvements.flush();
                    for (Dossier dossier : famille) {
                        System.out.println("Dossier " + dossier.getId() + " : "
                                + mouvements.findActifsByDossierIdOrderByIdAsc(dossier.getId()).size()
                                + " mouvements actifs, phase=" + dossier.getPhaseDossier().getNom()
                                + ", statut=" + dossier.getStatutDossier().getNom());
                    }
                }
            });
        }
    }
}
