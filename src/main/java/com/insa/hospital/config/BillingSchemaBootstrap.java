package com.insa.hospital.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BillingSchemaBootstrap {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureBillingSchema() {
        ensurePaymentCategoryExtensions();
        ensureServiceChargeTable();
    }

    private void ensurePaymentCategoryExtensions() {
        jdbcTemplate.execute("ALTER TABLE payment_category ADD COLUMN IF NOT EXISTS service_group VARCHAR(100)");
        jdbcTemplate.execute("ALTER TABLE payment_category ADD COLUMN IF NOT EXISTS service_role VARCHAR(100)");
        jdbcTemplate.execute("ALTER TABLE payment_category ADD COLUMN IF NOT EXISTS revenue_target VARCHAR(100)");
        jdbcTemplate.execute("ALTER TABLE payment_category ADD COLUMN IF NOT EXISTS active BOOLEAN");

        jdbcTemplate.execute("""
                UPDATE payment_category
                SET active = TRUE
                WHERE active IS NULL
                """);

        jdbcTemplate.execute("""
                UPDATE payment_category
                SET service_group = CASE
                    WHEN service_group IS NOT NULL AND TRIM(service_group) <> '' THEN service_group
                    WHEN LOWER(COALESCE(type, '')) = 'diagnostic' THEN 'Laboratory'
                    ELSE 'General'
                END
                WHERE service_group IS NULL OR TRIM(service_group) = ''
                """);

        jdbcTemplate.execute("""
                UPDATE payment_category
                SET service_role = CASE
                    WHEN service_role IS NOT NULL AND TRIM(service_role) <> '' THEN service_role
                    WHEN LOWER(COALESCE(type, '')) = 'diagnostic' THEN 'Laboratory'
                    ELSE 'Hospital'
                END
                WHERE service_role IS NULL OR TRIM(service_role) = ''
                """);

        jdbcTemplate.execute("""
                UPDATE payment_category
                SET revenue_target = CASE
                    WHEN revenue_target IS NOT NULL AND TRIM(revenue_target) <> '' THEN revenue_target
                    WHEN LOWER(COALESCE(type, '')) = 'diagnostic' THEN 'Diagnostic Revenue'
                    ELSE 'Hospital Revenue'
                END
                WHERE revenue_target IS NULL OR TRIM(revenue_target) = ''
                """);

        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_payment_category_scope_active
                ON payment_category (hospital_id, active, id DESC)
                """);
    }

    private void ensureServiceChargeTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS service_charge (
                    id BIGSERIAL PRIMARY KEY,
                    hospital_id VARCHAR(100) NOT NULL,
                    visit_id VARCHAR(100),
                    patient_id VARCHAR(100) NOT NULL,
                    patient_name VARCHAR(255),
                    service_catalog_id INTEGER,
                    service_name VARCHAR(255) NOT NULL,
                    service_group VARCHAR(100),
                    service_role VARCHAR(100),
                    revenue_target VARCHAR(100),
                    responsible_doctor_id VARCHAR(100),
                    responsible_doctor_name VARCHAR(255),
                    ordering_staff_id VARCHAR(100),
                    ordering_staff_name VARCHAR(255),
                    ordering_staff_role VARCHAR(100),
                    unit_price VARCHAR(100) NOT NULL,
                    quantity INTEGER NOT NULL DEFAULT 1,
                    total_amount VARCHAR(100) NOT NULL,
                    paid_amount VARCHAR(100) NOT NULL DEFAULT '0',
                    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
                    notes VARCHAR(1000),
                    charged_at VARCHAR(100) NOT NULL,
                    charged_date VARCHAR(20) NOT NULL,
                    created_at VARCHAR(100) NOT NULL,
                    updated_at VARCHAR(100) NOT NULL
                )
                """);

        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_service_charge_scope_date
                ON service_charge (hospital_id, charged_date DESC, id DESC)
                """);

        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_service_charge_patient
                ON service_charge (hospital_id, patient_id, id DESC)
                """);

        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_service_charge_visit
                ON service_charge (hospital_id, visit_id, id DESC)
                """);

        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_service_charge_status
                ON service_charge (hospital_id, status, id DESC)
                """);
    }
}
