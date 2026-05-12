package com.insa.hospital.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SettingsBrandingSchemaBootstrap {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureBrandingColumnsExist() {
        jdbcTemplate.execute("""
                ALTER TABLE settings
                    ADD COLUMN IF NOT EXISTS homepage_title VARCHAR(500),
                    ADD COLUMN IF NOT EXISTS homepage_description TEXT,
                    ADD COLUMN IF NOT EXISTS footer_text TEXT,
                    ADD COLUMN IF NOT EXISTS footer_tagline VARCHAR(500),
                    ADD COLUMN IF NOT EXISTS primary_color VARCHAR(20),
                    ADD COLUMN IF NOT EXISTS primary_dark_color VARCHAR(20),
                    ADD COLUMN IF NOT EXISTS accent_color VARCHAR(20),
                    ADD COLUMN IF NOT EXISTS background_color VARCHAR(20),
                    ADD COLUMN IF NOT EXISTS surface_color VARCHAR(20),
                    ADD COLUMN IF NOT EXISTS text_color VARCHAR(20)
                """);
    }
}
