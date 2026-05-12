package com.insa.hospital.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AllianceHospitalSchemaBootstrap {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureAllianceHospitalTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS alliance_hospitals (
                    id BIGSERIAL PRIMARY KEY,
                    hospital_id VARCHAR(100) NOT NULL,
                    name VARCHAR(255) NOT NULL,
                    address VARCHAR(255) NOT NULL,
                    phone VARCHAR(50) NOT NULL,
                    email VARCHAR(255),
                    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT chk_alliance_hospital_status
                        CHECK (status IN ('ACTIVE', 'INACTIVE'))
                )
                """);

        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_alliance_hospitals_hospital_id
                ON alliance_hospitals (hospital_id)
                """);

        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_alliance_hospitals_scope_status
                ON alliance_hospitals (hospital_id, status)
                """);
    }
}
