package com.insa.hospital.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PatientVisitSchemaBootstrap {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensurePatientVisitTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS patient_visit (
                    id BIGSERIAL PRIMARY KEY,
                    patient_id VARCHAR(100) NOT NULL,
                    reason_for_visit VARCHAR(500),
                    status VARCHAR(50) NOT NULL,
                    visit_type VARCHAR(50) NOT NULL DEFAULT 'NORMAL',
                    assigned_department_id VARCHAR(100),
                    assigned_department_name VARCHAR(255),
                    assigned_doctor_id VARCHAR(100),
                    assigned_doctor_name VARCHAR(255),
                    assigned_nurse_id VARCHAR(100),
                    assigned_nurse_name VARCHAR(255),
                    doctor_id VARCHAR(100),
                    doctor_name VARCHAR(255),
                    nurse_id VARCHAR(100),
                    nurse_name VARCHAR(255),
                    department_id VARCHAR(100),
                    department_name VARCHAR(255),
                    prescription_id VARCHAR(100),
                    lab_order_id VARCHAR(100),
                    assigned_to VARCHAR(255),
                    assigned_type VARCHAR(50),
                    emergency_priority VARCHAR(50),
                    emergency_assessment VARCHAR(4000),
                    emergency_treatment VARCHAR(4000),
                    emergency_disposition VARCHAR(255),
                    emergency_disposition_note VARCHAR(2000),
                    emergency_stage VARCHAR(100),
                    emergency_stage_note VARCHAR(2000),
                    emergency_medication_summary VARCHAR(4000),
                    emergency_billing_amount VARCHAR(100),
                    emergency_billing_note VARCHAR(2000),
                    emergency_referral_destination VARCHAR(500),
                    emergency_follow_up_instruction VARCHAR(2000),
                    emergency_completed_at VARCHAR(100),
                    created_at VARCHAR(100) NOT NULL,
                    updated_at VARCHAR(100) NOT NULL,
                    hospital_id VARCHAR(100) NOT NULL
                )
                """);

        // Ensure all columns exist (for tables created by older versions)
        String[] columns = {
            "visit_type VARCHAR(50) DEFAULT 'NORMAL'",
            "assigned_to VARCHAR(255)",
            "assigned_type VARCHAR(50)",
            "prescription_id VARCHAR(100)",
            "lab_order_id VARCHAR(100)",
            "doctor_id VARCHAR(100)",
            "doctor_name VARCHAR(255)",
            "nurse_id VARCHAR(100)",
            "nurse_name VARCHAR(255)",
            "department_id VARCHAR(100)",
            "department_name VARCHAR(255)",
            "emergency_priority VARCHAR(50)",
            "emergency_assessment VARCHAR(4000)",
            "emergency_treatment VARCHAR(4000)",
            "emergency_disposition VARCHAR(255)",
            "emergency_disposition_note VARCHAR(2000)",
            "emergency_stage VARCHAR(100)",
            "emergency_stage_note VARCHAR(2000)",
            "emergency_medication_summary VARCHAR(4000)",
            "emergency_billing_amount VARCHAR(100)",
            "emergency_billing_note VARCHAR(2000)",
            "emergency_referral_destination VARCHAR(500)",
            "emergency_follow_up_instruction VARCHAR(2000)",
            "emergency_completed_at VARCHAR(100)"
        };
        for (String colDef : columns) {
            jdbcTemplate.execute("ALTER TABLE patient_visit ADD COLUMN IF NOT EXISTS " + colDef);
        }

        jdbcTemplate.execute("""
                UPDATE patient_visit
                SET visit_type = 'NORMAL'
                WHERE visit_type IS NULL OR TRIM(visit_type) = ''
                """);

        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_patient_visit_scope_status
                ON patient_visit (hospital_id, status, id DESC)
                """);

        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_patient_visit_scope_type_status
                ON patient_visit (hospital_id, visit_type, status, id DESC)
                """);

        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_patient_visit_patient_status
                ON patient_visit (patient_id, status, id DESC)
                """);
    }
}
