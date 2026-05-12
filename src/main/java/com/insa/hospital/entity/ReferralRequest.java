package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity mapping the `referral_requests` table.
 *
 * Represents a corporate referral request within the INSA Medical Insurance
 * & ERP system. Two entry points produce these records:
 *   1. Clinical — A Doctor refers a patient during examination.
 *   2. Self-Service — An INSA employee requests a referral for themselves
 *      or a registered family member.
 *
 * Both flows land in PENDING status and require Admin approval before
 * the patient can be seen at the destination hospital.
 *
 * CRITICAL: ddl-auto is set to 'none'. This entity assumes the table
 * `referral_requests` already exists in the PostgreSQL database.
 * Create it manually using the companion SQL migration script.
 */
@Entity
@Table(name = "referral_requests")
@Getter
@Setter
@NoArgsConstructor
public class ReferralRequest {

    // ─── Enums ───────────────────────────────────────────────────────────────

    /**
     * Who initiated the referral request.
     *   DOCTOR   — Clinical referral during patient examination.
     *   EMPLOYEE — Self-service referral by an INSA staff member.
     */
    public enum RequestedByRole {
        DOCTOR, EMPLOYEE
    }

    /**
     * Lifecycle status of the referral request.
     *   PENDING   — Awaiting admin review.
     *   APPROVED  — Admin approved; referral letter can be printed.
     *   REJECTED  — Admin denied the request (reason stored in clinicalNotes).
     *   COMPLETED — External bill has been settled / expense tracked by Finance.
     */
    public enum ReferralStatus {
        PENDING, APPROVED, REJECTED, COMPLETED
    }

    // ─── Columns ─────────────────────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Legacy 6-digit patient ID (e.g. "727265") or the integer PK as string.
     * References the `patient` table — stored as String for legacy compatibility.
     */
    @Column(name = "patient_id", length = 100, nullable = false)
    private String patientId;

    /**
     * Role of the person who submitted this referral.
     * Stored as the enum name string: "DOCTOR" or "EMPLOYEE".
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "requested_by_role", length = 20, nullable = false)
    private RequestedByRole requestedByRole;

    /**
     * The ion_user_id (legacy user PK) of the staff member who created
     * this referral — could be a Doctor or an Employee.
     */
    @Column(name = "requested_by_user_id", length = 100, nullable = false)
    private String requestedByUserId;

    /** Name of the external hospital the patient is being referred to. */
    @Column(name = "destination_hospital", length = 500, nullable = false)
    private String destinationHospital;

    /** Clinical or administrative reason for the referral. */
    @Column(name = "reason_for_referral", columnDefinition = "TEXT", nullable = false)
    private String reasonForReferral;

    /**
     * Optional clinical notes — diagnosis details, doctor observations, etc.
     * May also hold the rejection reason when status is REJECTED.
     */
    @Column(name = "clinical_notes", columnDefinition = "TEXT")
    private String clinicalNotes;

    /**
     * Current lifecycle status. Defaults to PENDING on creation.
     * Stored as the enum name string: "PENDING", "APPROVED", "REJECTED", "COMPLETED".
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ReferralStatus status;

    /**
     * ISO-8601 date string when the referral was approved (e.g. "2026-04-02").
     * NULL until the admin approves or rejects.
     */
    @Column(name = "approval_date", length = 50)
    private String approvalDate;

    @Column(name = "approved_by_user_id", length = 100)
    private String approvedByUserId;

    @Column(name = "approved_by_name", length = 255)
    private String approvedByName;

    /**
     * Amount billed by the external hospital (in ETB).
     * Set when Finance settles the bill (status → COMPLETED).
     */
    @Column(name = "external_bill_amount")
    private Double externalBillAmount;

    /**
     * URL or file path of the uploaded bill/invoice document.
     * Set when Finance settles the bill.
     */
    @Column(name = "bill_document_url", length = 1000)
    private String billDocumentUrl;

    /**
     * URL or file path of the document uploaded when an approved employee
     * medical request is sent to letter management.
     */
    @Column(name = "letter_management_document_url", length = 1000)
    private String letterManagementDocumentUrl;

    /**
     * ISO-8601 timestamp of when the request was sent to letter management.
     */
    @Column(name = "letter_management_sent_at", length = 50)
    private String letterManagementSentAt;

    /**
     * User id of the admin/superadmin who sent the request to letter management.
     */
    @Column(name = "letter_management_sent_by_user_id", length = 100)
    private String letterManagementSentByUserId;

    /**
     * Multi-tenant hospital identifier — scopes all queries.
     * Resolved from the JWT token's hospitalId claim.
     */
    @Column(name = "hospital_id", length = 100, nullable = false)
    private String hospitalId;

    /**
     * ISO-8601 timestamp of when this referral was created (e.g. "2026-04-02T13:28:56").
     * Set by the service layer at creation time.
     */
    @Column(name = "created_at", length = 50)
    private String createdAt;
}
