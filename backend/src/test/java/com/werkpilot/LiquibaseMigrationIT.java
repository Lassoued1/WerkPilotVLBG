package com.werkpilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.werkpilot.support.PostgreSqlTestContainerSupport;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
class LiquibaseMigrationIT extends PostgreSqlTestContainerSupport {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void appliesRootChangelogToPostgreSql() throws Exception {
        assertThat(dataSource.getConnection().getMetaData().getDatabaseProductName())
                .isEqualTo("PostgreSQL");

        Integer changeLogRows = jdbcTemplate.queryForObject("select count(*) from databasechangelog", Integer.class);
        assertThat(changeLogRows).isGreaterThanOrEqualTo(2);

        Boolean pgcryptoEnabled = jdbcTemplate.queryForObject(
                "select exists (select 1 from pg_extension where extname = 'pgcrypto')",
                Boolean.class);
        assertThat(pgcryptoEnabled).isTrue();
    }
}
