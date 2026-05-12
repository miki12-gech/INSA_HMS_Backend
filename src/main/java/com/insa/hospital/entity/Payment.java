package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity mapping the legacy `payment` table (Finance / Billing).
 *
 * Column mapping from nhospital1 SQL dump (lines 1408–1435).
 * ALL 27 columns mapped 1:1 — DO NOT add, rename, or remove columns.
 *
 * ══════════════════════════════════════════════════════════════
 *  CRITICAL LEGACY DESIGN: category_amount column
 * ══════════════════════════════════════════════════════════════
 *  Line items are stored as a single delimited string — NOT in a
 *  separate child table. Per agent.md §6:
 *
 *    Format per item: paymentCategoryId*price*type*count
 *    Multiple items separated by comma ","
 *
 *  Example (from actual data row 2025):
 *    "88*350*diagnostic*1,90*350*diagnostic*1,89*350*diagnostic*1"
 *
 *  The service layer parses/serializes this. The entity stores raw.
 *
 * Key financial fields:
 *  - amount        : sub-total (sum of all items before discount/VAT)
 *  - discount      : discount percentage string (e.g. "0", "10")
 *  - flat_discount : discount as fixed amount
 *  - vat           : VAT percentage string
 *  - flat_vat      : VAT as fixed amount
 *  - gross_total   : final payable amount (after discount/VAT)
 *  - hospital_amount: hospital share of payment
 *  - doctor_amount : doctor commission share
 *  - amount_received: what was actually collected
 *  - status        : 'unpaid' | 'paid'
 *  - deposit_type  : 'Cash' | 'Online' | etc.
 *  - patient       : varchar → patient.id
 *  - doctor        : varchar → doctor.id
 *  - user          : varchar → users.id (cashier who created the bill)
 * ══════════════════════════════════════════════════════════════
 */
@Entity
@Table(name = "payment")
@Getter
@Setter
@NoArgsConstructor
public class Payment {

    /** Delimiter separating line items in category_amount */
    public static final String ITEM_SEPARATOR = ",";
    /** Delimiter separating fields within one line item */
    public static final String FIELD_SEPARATOR = "*";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /** Legacy nullable category field. */
    @Column(name = "category", length = 100)
    private String category;

    /** References patient.id as varchar string. */
    @Column(name = "patient", length = 100)
    private String patient;

    /** References doctor.id as varchar string. */
    @Column(name = "doctor", length = 100)
    private String doctor;

    /** Unix epoch string — when the payment was created. */
    @Column(name = "date", length = 100)
    private String date;

    /** Sub-total before discount (all line items summed). */
    @Column(name = "amount", length = 100)
    private String amount;

    /** VAT percentage (e.g. "0", "15"). Default '0'. */
    @Column(name = "vat", length = 100, nullable = false)
    private String vat = "0";

    /** X-ray related field (legacy). */
    @Column(name = "x_ray", length = 100)
    private String xRay;

    /** VAT as fixed flat amount. */
    @Column(name = "flat_vat", length = 100)
    private String flatVat;

    /** Discount percentage (e.g. "0", "10"). Default '0'. */
    @Column(name = "discount", length = 100, nullable = false)
    private String discount = "0";

    /** Discount as fixed flat amount. */
    @Column(name = "flat_discount", length = 100)
    private String flatDiscount;

    /** Final payable amount after discount/VAT applied. */
    @Column(name = "gross_total", length = 100)
    private String grossTotal;

    /** Remarks / notes from cashier. */
    @Column(name = "remarks", length = 500)
    private String remarks;

    /** Hospital's revenue share. */
    @Column(name = "hospital_amount", length = 100)
    private String hospitalAmount;

    /** Doctor's commission share. */
    @Column(name = "doctor_amount", length = 100)
    private String doctorAmount;

    /**
     * Delimited line items string.
     * Format: paymentCategoryId*price*type*count,...
     * Example: "88*350*diagnostic*1,90*350*diagnostic*1"
     * Parse/serialize in PaymentService, never raw-build here.
     */
    @Column(name = "category_amount", length = 1000)
    private String categoryAmount;

    /** Legacy field (appears to mirror category_amount names). */
    @Column(name = "category_name", length = 1000)
    private String categoryName;

    /** Amount actually received from patient. */
    @Column(name = "amount_received", length = 100)
    private String amountReceived;

    /** Payment method: 'Cash', 'Online', 'Card', etc. */
    @Column(name = "deposit_type", length = 100)
    private String depositType;

    /** 'unpaid' | 'paid' */
    @Column(name = "status", length = 100)
    private String status;

    /** users.id of cashier/accountant who created the invoice. */
    @Column(name = "\"user\"", length = 100)
    private String user;

    /** Denormalized patient name — set at billing time. */
    @Column(name = "patient_name", length = 100)
    private String patientName;

    /** Denormalized patient phone. */
    @Column(name = "patient_phone", length = 100)
    private String patientPhone;

    /** Denormalized patient address. */
    @Column(name = "patient_address", length = 100)
    private String patientAddress;

    /** Denormalized doctor name — set at billing time. */
    @Column(name = "doctor_name", length = 100)
    private String doctorName;

    /** Human-readable date string (e.g. "17-12-21"). */
    @Column(name = "date_string", length = 100)
    private String dateString;

    /** Multi-tenant hospital identifier. */
    @Column(name = "hospital_id", length = 100)
    private String hospitalId;
}
