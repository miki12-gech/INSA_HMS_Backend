package com.insa.hospital.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Automatically converts Render / Heroku style DATABASE_URL
 * (postgres://user:pass@host:port/dbname) into a JDBC-compatible
 * DataSource so Spring Boot + Hibernate can connect without issues.
 */
@Configuration
public class DatabaseConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        DataSourceProperties props = new DataSourceProperties();

        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl != null && !databaseUrl.isEmpty()) {
            try {
                // Render gives: postgres://user:pass@host:port/dbname
                // We need:      jdbc:postgresql://host:port/dbname
                URI uri = new URI(databaseUrl.replace("postgres://", "postgresql://"));

                String jdbcUrl = "jdbc:postgresql://" + uri.getHost()
                        + ":" + uri.getPort()
                        + uri.getPath();

                if (uri.getQuery() != null) {
                    jdbcUrl += "?" + uri.getQuery();
                }

                props.setUrl(jdbcUrl);

                if (uri.getUserInfo() != null) {
                    String[] userInfo = uri.getUserInfo().split(":", 2);
                    props.setUsername(userInfo[0]);
                    if (userInfo.length > 1) {
                        props.setPassword(userInfo[1]);
                    }
                }

                props.setDriverClassName("org.postgresql.Driver");
            } catch (URISyntaxException e) {
                throw new RuntimeException("Invalid DATABASE_URL: " + databaseUrl, e);
            }
        }

        return props;
    }
}
