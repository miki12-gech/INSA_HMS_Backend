package com.insa.hospital.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Directly creates the DataSource bean, handling Render's DATABASE_URL format
 * (postgres://user:pass@host:port/dbname) automatically.
 *
 * When DATABASE_URL is present → parse and use it (Render / production).
 * When DATABASE_URL is absent  → fall back to localhost defaults (local dev).
 */
@Configuration
public class DatabaseConfig {

    @Bean
    @Primary
    public DataSource dataSource() {
        String databaseUrl = System.getenv("DATABASE_URL");

        HikariDataSource ds = new HikariDataSource();
        ds.setDriverClassName("org.postgresql.Driver");

        if (databaseUrl != null && !databaseUrl.isEmpty()) {
            try {
                // Render gives:  postgres://user:pass@host:port/dbname
                // JDBC needs:    jdbc:postgresql://host:port/dbname
                String cleanUrl = databaseUrl;
                if (cleanUrl.startsWith("postgres://")) {
                    cleanUrl = cleanUrl.replace("postgres://", "postgresql://");
                }
                if (!cleanUrl.startsWith("postgresql://")) {
                    // already jdbc: format or something else
                    ds.setJdbcUrl(cleanUrl.startsWith("jdbc:") ? cleanUrl : "jdbc:" + cleanUrl);
                } else {
                    URI uri = new URI(cleanUrl);

                    String jdbcUrl = "jdbc:postgresql://" + uri.getHost();
                    if (uri.getPort() != -1) {
                        jdbcUrl += ":" + uri.getPort();
                    }
                    jdbcUrl += uri.getPath();

                    // Preserve query parameters (e.g. ?sslmode=require)
                    if (uri.getQuery() != null) {
                        jdbcUrl += "?" + uri.getQuery();
                    }

                    ds.setJdbcUrl(jdbcUrl);

                    if (uri.getUserInfo() != null) {
                        String[] userInfo = uri.getUserInfo().split(":", 2);
                        ds.setUsername(userInfo[0]);
                        if (userInfo.length > 1) {
                            ds.setPassword(userInfo[1]);
                        }
                    }
                }

                System.out.println("==> DatabaseConfig: using DATABASE_URL (production)");
            } catch (URISyntaxException e) {
                throw new RuntimeException("Invalid DATABASE_URL format: " + databaseUrl, e);
            }
        } else {
            // Local development defaults
            ds.setJdbcUrl("jdbc:postgresql://localhost:5432/insa_hospital");
            ds.setUsername("postgres");
            ds.setPassword("12345");
            System.out.println("==> DatabaseConfig: using localhost defaults (development)");
        }

        return ds;
    }
}
