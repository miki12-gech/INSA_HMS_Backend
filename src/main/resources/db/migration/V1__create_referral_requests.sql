-- ═══════════════════════════════════════════════════════════════════════════
--  REFERRAL REQUESTS TABLE — Corporate Referral & External Billing Module
--  Database: insa_hospital (PostgreSQL)
--
--  This table supports the INSA Corporate Medical Insurance ERP workflow:
--    1. Clinical entry — Doctor refers a patient during examination.
--    2. Self-Service   — Employee requests referral for self/family.
--    3. Admin approval — ADMIN approves/rejects the referral.
--    4. Bill settlement — Finance records the external hospital expense.
--
--  Run this script manually against your PostgreSQL database.
--  ddl-auto is set to 'none' — Hibernate will NOT create this table.
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS referral_requests (
    id                    BIGSERIAL       PRIMARY KEY,
    patient_id            VARCHAR(100)    NOT NULL,
    requested_by_role     VARCHAR(20)     NOT NULL,       -- 'DOCTOR' or 'EMPLOYEE'
    requested_by_user_id  VARCHAR(100)    NOT NULL,
    destination_hospital  VARCHAR(500)    NOT NULL,
    reason_for_referral   TEXT            NOT NULL,
    clinical_notes        TEXT,
    status                VARCHAR(20)     NOT NULL DEFAULT 'PENDING',  -- PENDING, APPROVED, REJECTED, COMPLETED
    approval_date         VARCHAR(50),
    external_bill_amount  DOUBLE PRECISION,
    bill_document_url     VARCHAR(1000),
    hospital_id           VARCHAR(100)    NOT NULL,
    created_at            VARCHAR(50),

    -- ── CONSTRAINTS ──────────────────────────────────────────────────────
    CONSTRAINT chk_referral_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'COMPLETED')),
    CONSTRAINT chk_requested_by_role
        CHECK (requested_by_role IN ('DOCTOR', 'EMPLOYEE'))
);

-- ── INDEXES for common query patterns ────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_referral_hospital_id
    ON referral_requests (hospital_id);

CREATE INDEX IF NOT EXISTS idx_referral_status_hospital
    ON referral_requests (status, hospital_id);

CREATE INDEX IF NOT EXISTS idx_referral_patient_hospital
    ON referral_requests (patient_id, hospital_id);

CREATE INDEX IF NOT EXISTS idx_referral_requester_hospital
    ON referral_requests (requested_by_user_id, hospital_id);

-- ═══════════════════════════════════════════════════════════════════════════
--  COMMENTS
-- ═══════════════════════════════════════════════════════════════════════════

COMMENT ON TABLE referral_requests IS
    'Corporate referral requests for INSA Medical Insurance & ERP system';

COMMENT ON COLUMN referral_requests.patient_id IS
    'References patient.id or patient.patient_id (legacy 6-digit) as varchar';

COMMENT ON COLUMN referral_requests.requested_by_role IS
    'Who initiated: DOCTOR (clinical) or EMPLOYEE (self-service)';

COMMENT ON COLUMN referral_requests.status IS
    'Lifecycle: PENDING → APPROVED/REJECTED → COMPLETED (bill settled)';

COMMENT ON COLUMN referral_requests.external_bill_amount IS
    'Amount in ETB billed by the destination hospital (set on settlement)';
