package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity mapping the legacy `ot_payment` table.
 *
 * ══════════════════════════════════════════════════════════════
 *  OT Payment — Operating Theatre Billing
 * ══════════════════════════════════════════════════════════════
 *
 * This is an entirely SEPARATE billing flow from the general
 * `payment` table. OT procedures involve up to 4 doctors
 * (chief surgeon + 2 assistant surgeons + anaesthesiologist)
 * each with their own fee column.
 *
 * Columns derived from audit report (finance.php lines 551–698):
 *   patient, doctor_c_s (chief surgeon), doctor_a_s_1, doctor_a_s_2,
 *   doctor_anaes, n_o_o (nature of operation), c_s_f, a_s_f_1,
 *   a_s_f_2, anaes_f, ot_charge, cab_rent, seat_rent, others,
 *   discount, vat, amount, doctor_fees, hospital_fees, gross_total,
 *   status, date, hospital_id, user, deposit_type, amount_received
 *
 * ══════════════════════════════════════════════════════════════
 *  CRITICAL FEE FORMULA (Audit Alert 3):
 * ══════════════════════════════════════════════════════════════
 *  doctor_fees   = c_s_f + a_s_f_1 + a_s_f_2 + anaes_f
 *  hospital_fees = ot_charge + cab_rent + seat_rent + others
 *  amount        = doctor_fees + hospital_fees
 *
 *  Discount deduction applies ONLY to hospital_fees (never doctor_fees):
 *    if discount_type == 'flat':
 *        hospital_fees -= flat_discount_amount
 *    else (percentage):
 *        hospital_fees -= amount * (discount% / 100)
 *
 *  gross_total = (doctor_fees + adjusted_hospital_fees) + VAT
 *
 *  PatientDeposit: amountReceivedId = "{id}.ot"  (NOT ".gp")
 * ══════════════════════════════════════════════════════════════
 */
@Entity
@Table(name = "ot_payment")
@Getter
@Setter
@NoArgsConstructor
public class OtPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /** References patient.id as varchar. */
    @Column(name = "patient", length = 100)
    private String patient;

    /** Chief Surgeon — references doctor.id as varchar. */
    @Column(name = "doctor_c_s", length = 100)
    private String doctorCs;

    /** Assistant Surgeon 1 — references doctor.id as varchar. */
    @Column(name = "doctor_a_s_1", length = 100)
    private String doctorAs1;

    /** Assistant Surgeon 2 — references doctor.id as varchar. */
    @Column(name = "doctor_a_s_2", length = 100)
    private String doctorAs2;

    /** Anaesthesiologist — references doctor.id as varchar. */
    @Column(name = "doctor_anaes", length = 100)
    private String doctorAnaes;

    /**
     * Nature Of Operation — free text description of the procedure.
     * e.g. "Appendectomy", "Caesarean Section"
     */
    @Column(name = "n_o_o", length = 500)
    private String noo;

    // ── Doctor Fee Fields (sum → doctor_fees) ────────────────────────────────

    /** Chief Surgeon Fee. */
    @Column(name = "c_s_f", length = 100)
    private String csf;

    /** Assistant Surgeon 1 Fee. */
    @Column(name = "a_s_f_1", length = 100)
    private String asf1;

    /** Assistant Surgeon 2 Fee. */
    @Column(name = "a_s_f_2", length = 100)
    private String asf2;

    /** Anaesthesiologist Fee. */
    @Column(name = "anaes_f", length = 100)
    private String anaesF;

    // ── Hospital Fee Fields (sum → hospital_fees) ─────────────────────────────

    /** OT Room Charge. */
    @Column(name = "ot_charge", length = 100)
    private String otCharge;

    /** Cabinet Rent. */
    @Column(name = "cab_rent", length = 100)
    private String cabRent;

    /** Seat/Bed Rent. */
    @Column(name = "seat_rent", length = 100)
    private String seatRent;

    /** Other miscellaneous hospital charges. */
    @Column(name = "others", length = 100)
    private String others;

    // ── Calculated Totals (set server-side — NEVER trust frontend) ────────────

    /** Discount percentage (e.g. "10"). */
    @Column(name = "discount", length = 100)
    private String discount;

    /** VAT percentage (e.g. "5"). */
    @Column(name = "vat", length = 100)
    private String vat;

    /**
     * Subtotal = doctor_fees + hospital_fees (before discount/VAT).
     * Calculated server-side.
     */
    @Column(name = "amount", length = 100)
    private String amount;

    /**
     * Doctor fees total = c_s_f + a_s_f_1 + a_s_f_2 + anaes_f.
     * IMPORTANT: Discount NEVER reduces this value.
     */
    @Column(name = "doctor_fees", length = 100)
    private String doctorFees;

    /**
     * Hospital fees total = ot_charge + cab_rent + seat_rent + others.
     * IMPORTANT: Discount reduces ONLY this value.
     */
    @Column(name = "hospital_fees", length = 100)
    private String hospitalFees;

    /**
     * Final gross total after discount and VAT.
     * gross_total = (doctor_fees + discounted_hospital_fees) * (1 + vat/100)
     */
    @Column(name = "gross_total", length = 100)
    private String grossTotal;

    /** "paid" | "unpaid". */
    @Column(name = "status", length = 100)
    private String status;

    /** Unix epoch string — date of OT procedure. */
    @Column(name = "date", length = 100)
    private String date;

    /** Payment method: "Cash", "Card", etc. */
    @Column(name = "deposit_type", length = 100)
    private String depositType;

    /** Amount received at time of billing. */
    @Column(name = "amount_received", length = 100)
    private String amountReceived;

    /** References the user (cashier/receptionist) who created this OT bill. */
    @Column(name = "\"user\"", length = 100)
    private String user;

    /** Multi-tenant hospital identifier. */
    @Column(name = "hospital_id", length = 100)
    private String hospitalId;
}
