package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity mapping the legacy `patient_deposit` table.
 *
 * ══════════════════════════════════════════════════════════════
 *  AUDIT FIX — Alert 5: patient_deposit double-write
 * ══════════════════════════════════════════════════════════════
 *  Every payment creation must atomically write BOTH:
 *   1. A `payment` row (the invoice)
 *   2. A `patient_deposit` row (the deposit receipt)
 *
 *  Legacy code: finance/controllers/finance.php lines 358–374
 *
 *  The `amount_received_id` column uses a composite key pattern:
 *   - "{paymentId}.gp"  → General Payment deposit (payment table)
 *   - "{paymentId}.ot"  → OT Payment deposit (ot_payment table)
 *
 *  This allows the legacy frontend to look up deposits by invoice
 *  and display "Amount Collected" on the invoice printout.
 *
 *  WARNING: Do NOT rename `amount_received_id` — it is used as a
 *  business key throughout the finance module.
 * ══════════════════════════════════════════════════════════════
 */
@Entity
@Table(name = "patient_deposit")
@Getter
@Setter
@NoArgsConstructor
public class PatientDeposit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /**
     * Unix epoch string when this deposit was recorded.
     * Set automatically at creation time.
     */
    @Column(name = "date", length = 100)
    private String date;

    /**
     * References patient.id as varchar.
     * May be null for walk-in/anonymous payments.
     */
    @Column(name = "patient", length = 100)
    private String patient;

    /**
     * Composite business key linking this deposit to its invoice.
     * Format: "{paymentId}.gp" for general payments.
     *         "{otPaymentId}.ot" for OT payments.
     *
     * CRITICAL: This key is used by the legacy frontend to correlate
     * deposits with invoices. DO NOT change the format.
     */
    @Column(name = "amount_received_id", length = 100)
    private String amountReceivedId;

    /** References payment.id as varchar. */
    @Column(name = "payment_id", length = 100)
    private String paymentId;

    /** Cash amount deposited in this transaction. */
    @Column(name = "deposited_amount", length = 100)
    private String depositedAmount;

    /**
     * Payment method: "Cash", "Card", "Bank Transfer".
     * Sourced from the parent payment.deposit_type.
     */
    @Column(name = "deposit_type", length = 100)
    private String depositType;

    /**
     * References users.id (the cashier/receptionist who recorded this).
     */
    @Column(name = "\"user\"", length = 100)
    private String user;
}
