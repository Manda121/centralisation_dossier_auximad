package com.auximad.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;

/** Installs the PostgreSQL indexes after Hibernate has initialized the tables. */
@Component
public class DossierFullTextConfig implements ApplicationRunner {
    private final DataSource dataSource;

    public DossierFullTextConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String sql;
        try (var input = new ClassPathResource("db/dossier-full-text.sql").getInputStream()) {
            sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (var statement = connection.createStatement()) {
                statement.execute(sql);
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        }
    }
}
