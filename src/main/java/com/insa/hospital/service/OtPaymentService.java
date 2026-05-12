package com.insa.hospital.service;

import com.insa.hospital.entity.OtPayment;
import com.insa.hospital.entity.PatientDeposit;
import com.insa.hospital.exception.ResourceNotFoundException;
import com.insa.hospital.repository.OtPaymentRepository;
import com.insa.hospital.repository.PatientDepositRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * OT Payment Service — Operating Theatre Billing
 *
 * ══════════════════════════════════════════════════════════════
 *  AUDIT FIX — Missing Feature 2 & Alert 3: OT Payment Module
 * ══════════════════════════════════════════════════════════════
 *
 * Implements the EXACT legacy fee formula from:
 *   finance/controllers/finance.php lines 551–698
 *
 * ══════════════════════════════════════════════════════════════
 *  OT PAYMENT FEE FORMULA (STRICT, DO NOT ALTER):
 * ══════════════════════════════════════════════════════════════
 *
 *  Step 1 — Doctor Fees (sum of all surgeon/anaesthesiologist fees):
 *    doctor_fees = c_s_f + a_s_f_1 + a_s_f_2 + anaes_f
 *
 *  Step 2 — Hospital Fees (sum of all OT facility charges):
 *    hospital_fees = ot_charge + cab_rent + seat_rent + others
 *
 *  Step 3 — Subtotal (before discount/VAT):
 *    amount = doctor_fees + hospital_fees
 *
 *  Step 4 — Discount Application (CRITICAL RULE):
 *    The discount reduces ONLY hospital_fees, NEVER doctor_fees.
 *    (Legacy source: finance.php lines 561–568)
 *
 *    if discount_type == 'flat':
 *      flat_discount_amount = discount (fixed ETB amount)
 *      hospital_fees -= flat_discount_amount
 *    else (percentage):
 *      flat_discount_amount = amount × (discount% / 100)
 *      hospital_fees -= flat_discount_amount
 *
 *  Step 5 — Gross Total (after discount):
 *    adjusted_amount = doctor_fees + hospital_fees  (hospital_fees already discounted)
 *    gross_total = adjusted_amount + adjusted_amount × (vat% / 100)
 *
 *  Step 6 — PatientDeposit double-write:
 *    amountReceivedId = "{savedId}.ot"   ← ".ot" suffix, NOT ".gp"
 *    This is the forensic link used by the invoice printout.
 *
 * ══════════════════════════════════════════════════════════════
 *
 * @Transactional ensures that saving OtPayment AND PatientDeposit
 * either both succeed or both rollback atomically.
 */
@Service
@Transactional
public class OtPaymentService {

    private final OtPaymentRepository otPaymentRepository;
    private final PatientDepositRepository patientDepositRepository;

    @Autowired
    public OtPaymentService(OtPaymentRepository otPaymentRepository,
                             PatientDepositRepository patientDepositRepository) {
        this.otPaymentRepository = otPaymentRepository;
        this.patientDepositRepository = patientDepositRepository;
    }

    // ─── Create OT Invoice ─────────────────────────────────────────────────────

    /**
     * Creates an OT payment invoice with full server-side fee calculation.
     *
     * @param dto        OtPaymentRequestDto from controller
     * @param hospitalId from JWT (multi-tenant)
     * @param userId     from JWT (cashier who created the bill)
     */
    @Transactional
    public OtPayment createOtPayment(OtPaymentRequestDto dto,
                                     String hospitalId,
                                     String userId) {
        OtPayment p = new OtPayment();

        // ── Basic identifiers ────────────────────────────────────────────────────
        p.setPatient(dto.patient());
        p.setDoctorCs(nvl(dto.doctorCs()));
        p.setDoctorAs1(nvl(dto.doctorAs1()));
        p.setDoctorAs2(nvl(dto.doctorAs2()));
        p.setDoctorAnaes(nvl(dto.doctorAnaes()));
        p.setNoo(nvl(dto.noo()));
        p.setHospitalId(hospitalId);
        p.setUser(userId);
        p.setDepositType(StringUtils.hasText(dto.depositType()) ? dto.depositType() : "Cash");
        p.setAmountReceived(StringUtils.hasText(dto.amountReceived()) ? dto.amountReceived() : "0");
        p.setStatus("unpaid");
        p.setDate(String.valueOf(System.currentTimeMillis() / 1000L));

        // ── Store individual fee inputs ──────────────────────────────────────────
        p.setCsf(nvl(dto.csf()));
        p.setAsf1(nvl(dto.asf1()));
        p.setAsf2(nvl(dto.asf2()));
        p.setAnaesF(nvl(dto.anaesF()));
        p.setOtCharge(nvl(dto.otCharge()));
        p.setCabRent(nvl(dto.cabRent()));
        p.setSeatRent(nvl(dto.seatRent()));
        p.setOthers(nvl(dto.others()));
        p.setDiscount(StringUtils.hasText(dto.discount()) ? dto.discount() : "0");
        p.setVat(StringUtils.hasText(dto.vat()) ? dto.vat() : "0");

        // ════════════════════════════════════════════════════════════════════════
        //  SERVER-SIDE FEE CALCULATION (STRICT LEGACY FORMULA)
        //  Source: finance/controllers/finance.php lines 551–568
        // ════════════════════════════════════════════════════════════════════════

        // Step 1: Doctor fees = sum of all surgeon/anaesthesiologist fees
        double doctorFees = parse(dto.csf()) + parse(dto.asf1())
                          + parse(dto.asf2()) + parse(dto.anaesF());

        // Step 2: Hospital fees = sum of all OT facility charges
        double hospitalFees = parse(dto.otCharge()) + parse(dto.cabRent())
                            + parse(dto.seatRent()) + parse(dto.others());

        // Step 3: Subtotal (pre-discount, pre-VAT)
        double amount = doctorFees + hospitalFees;

        // Step 4: Discount — ONLY reduces hospital_fees (CRITICAL RULE)
        double discountPct    = parse(dto.discount());
        String discountType   = StringUtils.hasText(dto.discountType()) ? dto.discountType() : "percentage";
        double flatDiscount;

        if ("flat".equalsIgnoreCase(discountType)) {
            // Flat discount: the discount value IS the amount to deduct
            flatDiscount = discountPct;
        } else {
            // Percentage discount: calculate the flat amount from the full subtotal
            flatDiscount = amount * discountPct / 100.0;
        }

        // DEDUCT ONLY FROM hospital_fees — doctor fees are NEVER touched by discount
        hospitalFees = Math.max(0, hospitalFees - flatDiscount);

        // Step 5: Gross total after discount and VAT
        double adjustedAmount = doctorFees + hospitalFees;
        double vatPct = parse(dto.vat());
        double grossTotal = adjustedAmount + adjustedAmount * vatPct / 100.0;

        // ── Store calculated values ──────────────────────────────────────────────
        p.setDoctorFees(fmt(doctorFees));
        p.setHospitalFees(fmt(hospitalFees));    // already discounted
        p.setAmount(fmt(amount));                // pre-discount subtotal
        p.setGrossTotal(fmt(grossTotal));

        // ════════════════════════════════════════════════════════════════════════

        // ── Save main OT payment invoice ─────────────────────────────────────────
        OtPayment saved = otPaymentRepository.save(p);

        // ── PatientDeposit double-write (Alert 5 pattern, .ot suffix) ────────────
        // CRITICAL: amountReceivedId = "{id}.ot" (OT payment, NOT ".gp")
        PatientDeposit deposit = new PatientDeposit();
        deposit.setDate(String.valueOf(System.currentTimeMillis() / 1000L));
        deposit.setPatient(p.getPatient());
        deposit.setPaymentId(String.valueOf(saved.getId()));
        deposit.setAmountReceivedId(saved.getId() + ".ot");   // ".ot" suffix for OT
        deposit.setDepositedAmount(StringUtils.hasText(p.getAmountReceived()) ? p.getAmountReceived() : "0");
        deposit.setDepositType(StringUtils.hasText(p.getDepositType()) ? p.getDepositType() : "Cash");
        deposit.setUser(userId);
        patientDepositRepository.save(deposit);

        return saved;
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<OtPayment> listAll(String hospitalId, Pageable pageable) {
        return otPaymentRepository.findByHospitalId(hospitalId, pageable);
    }

    @Transactional(readOnly = true)
    public List<OtPayment> listByPatient(String patient, String hospitalId) {
        return otPaymentRepository.findByPatientAndHospitalId(patient, hospitalId);
    }

    @Transactional(readOnly = true)
    public Page<OtPayment> listByStatus(String status, String hospitalId, Pageable pageable) {
        return otPaymentRepository.findByStatusAndHospitalId(status, hospitalId, pageable);
    }

    @Transactional(readOnly = true)
    public OtPayment getById(Integer id, String hospitalId) {
        return otPaymentRepository.findById(id)
                .filter(p -> hospitalId.equals(p.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("OtPayment", "id", id));
    }

    // ─── Status Update ────────────────────────────────────────────────────────

    @Transactional
    public OtPayment updateStatus(Integer id, String status, String hospitalId) {
        OtPayment p = getById(id, hospitalId);
        p.setStatus(status);
        return otPaymentRepository.save(p);
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    @Transactional
    public void deleteOtPayment(Integer id, String hospitalId) {
        OtPayment p = getById(id, hospitalId);
        otPaymentRepository.delete(p);
    }

    // ─── Full Update ──────────────────────────────────────────────────────────

    /**
     * PUT /api/payments/ot/{id}
     *
     * Updates all mutable fields of an existing OT invoice and recalculates
     * fees using the same strict legacy formula as createOtPayment().
     * The linked PatientDeposit row (amountReceivedId = "{id}.ot") is also
     * updated to reflect the new amountReceived value.
     */
    @Transactional
    public OtPayment updateOtPayment(Integer id, OtPaymentRequestDto dto, String hospitalId) {
        OtPayment p = getById(id, hospitalId);

        // ── Update mutable identifiers ───────────────────────────────────────
        p.setPatient(dto.patient());
        p.setDoctorCs(nvl(dto.doctorCs()));
        p.setDoctorAs1(nvl(dto.doctorAs1()));
        p.setDoctorAs2(nvl(dto.doctorAs2()));
        p.setDoctorAnaes(nvl(dto.doctorAnaes()));
        p.setNoo(nvl(dto.noo()));
        p.setDepositType(StringUtils.hasText(dto.depositType()) ? dto.depositType() : "Cash");
        p.setAmountReceived(StringUtils.hasText(dto.amountReceived()) ? dto.amountReceived() : "0");

        // ── Store individual fee inputs ──────────────────────────────────────
        p.setCsf(nvl(dto.csf()));
        p.setAsf1(nvl(dto.asf1()));
        p.setAsf2(nvl(dto.asf2()));
        p.setAnaesF(nvl(dto.anaesF()));
        p.setOtCharge(nvl(dto.otCharge()));
        p.setCabRent(nvl(dto.cabRent()));
        p.setSeatRent(nvl(dto.seatRent()));
        p.setOthers(nvl(dto.others()));
        p.setDiscount(StringUtils.hasText(dto.discount()) ? dto.discount() : "0");
        p.setVat(StringUtils.hasText(dto.vat()) ? dto.vat() : "0");

        // ════════════════════════════════════════════════════════════════════
        //  SERVER-SIDE FEE RECALCULATION (identical to createOtPayment)
        // ════════════════════════════════════════════════════════════════════

        double doctorFees  = parse(dto.csf()) + parse(dto.asf1())
                           + parse(dto.asf2()) + parse(dto.anaesF());
        double hospitalFees = parse(dto.otCharge()) + parse(dto.cabRent())
                            + parse(dto.seatRent()) + parse(dto.others());
        double amount      = doctorFees + hospitalFees;

        double discountPct  = parse(dto.discount());
        String discountType = StringUtils.hasText(dto.discountType()) ? dto.discountType() : "percentage";
        double flatDiscount = "flat".equalsIgnoreCase(discountType)
                ? discountPct
                : amount * discountPct / 100.0;

        hospitalFees = Math.max(0, hospitalFees - flatDiscount);

        double adjustedAmount = doctorFees + hospitalFees;
        double vatPct   = parse(dto.vat());
        double grossTotal = adjustedAmount + adjustedAmount * vatPct / 100.0;

        p.setDoctorFees(fmt(doctorFees));
        p.setHospitalFees(fmt(hospitalFees));
        p.setAmount(fmt(amount));
        p.setGrossTotal(fmt(grossTotal));

        // ════════════════════════════════════════════════════════════════════

        OtPayment saved = otPaymentRepository.save(p);

        // ── Sync linked PatientDeposit row ───────────────────────────────────
        String depositRef = saved.getId() + ".ot";
        patientDepositRepository.findByAmountReceivedId(depositRef).ifPresent(deposit -> {
            deposit.setDepositedAmount(StringUtils.hasText(p.getAmountReceived()) ? p.getAmountReceived() : "0");
            deposit.setDepositType(StringUtils.hasText(p.getDepositType()) ? p.getDepositType() : "Cash");
            patientDepositRepository.save(deposit);
        });

        return saved;
    }

    // ─── Private Utilities ────────────────────────────────────────────────────

    private double parse(String s) {
        if (!StringUtils.hasText(s)) return 0.0;
        try { return Double.parseDouble(s.trim()); }
        catch (NumberFormatException e) { return 0.0; }
    }

    private String fmt(double v) {
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.format("%.2f", v);
    }

    private String nvl(String s) { return s != null ? s : ""; }

    // ─── Inner DTO ─────────────────────────────────────────────────────────────

    /**
     * OtPaymentRequestDto — accepted by OtPaymentController.POST and OtPaymentController.PUT
     */
    public record OtPaymentRequestDto(
            String patient,
            String doctorCs,
            String doctorAs1,
            String doctorAs2,
            String doctorAnaes,
            String noo,
            String csf,
            String asf1,
            String asf2,
            String anaesF,
            String otCharge,
            String cabRent,
            String seatRent,
            String others,
            String discount,
            String discountType,
            String vat,
            String depositType,
            String amountReceived
    ) {}
}
