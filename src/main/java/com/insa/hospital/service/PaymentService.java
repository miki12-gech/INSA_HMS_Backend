package com.insa.hospital.service;

import com.insa.hospital.dto.PaymentRequestDto;
import com.insa.hospital.dto.PaymentResponseDto;
import com.insa.hospital.entity.Doctor;
import com.insa.hospital.entity.Patient;
import com.insa.hospital.entity.PatientDeposit;
import com.insa.hospital.entity.Payment;
import com.insa.hospital.entity.PaymentCategory;
import com.insa.hospital.exception.ResourceNotFoundException;
import com.insa.hospital.repository.DoctorRepository;
import com.insa.hospital.repository.PatientDepositRepository;
import com.insa.hospital.repository.PatientRepository;
import com.insa.hospital.repository.PaymentCategoryRepository;
import com.insa.hospital.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Payment/Invoice Service — Business Logic Layer
 *
 * ══════════════════════════════════════════════════════════════
 *  CRITICAL: Server-Side Financial Calculation
 * ══════════════════════════════════════════════════════════════
 *  The service NEVER trusts frontend totals. For every invoice:
 *
 *  1. Iterate line items → sum (price × count) = subtotal (amount)
 *  2. Apply discount:
 *       if flatDiscount set  → grossTotal = subtotal - flatDiscount
 *       else if discount %   → grossTotal = subtotal * (1 - discount/100)
 *       else                 → grossTotal = subtotal
 *  3. Apply VAT if set (added on top of gross).
 *  4. Split commission per item type:
 *       'diagnostic' items: d_commission% → doctorAmount, remainder → hospitalAmount
 *       'others' items:     0% doctor     → all to hospitalAmount
 *
 * ══════════════════════════════════════════════════════════════
 *  Line item serialization (category_amount column):
 *    "categoryId*price*type*count,categoryId2*price2*type2*count2"
 * ══════════════════════════════════════════════════════════════
 */
@Service
@Transactional
public class PaymentService {

    private static final DateTimeFormatter DATE_STRING_FMT =
            DateTimeFormatter.ofPattern("dd-MM-yy");

    private final PaymentRepository paymentRepository;
    private final PaymentCategoryRepository paymentCategoryRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final PatientDepositRepository patientDepositRepository;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository,
                          PaymentCategoryRepository paymentCategoryRepository,
                          PatientRepository patientRepository,
                          DoctorRepository doctorRepository,
                          PatientDepositRepository patientDepositRepository) {
        this.paymentRepository = paymentRepository;
        this.paymentCategoryRepository = paymentCategoryRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.patientDepositRepository = patientDepositRepository;
    }

    // ─── Create Invoice ───────────────────────────────────────────────────────

    /**
     * Creates an invoice with full server-side financial validation.
     * @Transactional — full rollback if any step fails.
     *
     * Steps:
     *  1. Resolve all payment_category records for the provided item IDs
     *  2. Calculate subtotal = Σ (price × count) for all items
     *  3. Apply discount/VAT → grossTotal
     *  4. Compute hospital/doctor commission split
     *  5. Serialize line items → category_amount delimited string
     *  6. Denormalize patient/doctor names
     *  7. Save
     */
    @Transactional
    public PaymentResponseDto createPayment(PaymentRequestDto dto,
                                             String hospitalId,
                                             String cashierUserId) {
        Payment p = new Payment();
        p.setPatient(dto.patient());
        p.setDoctor(StringUtils.hasText(dto.doctor()) ? dto.doctor() : "0");
        p.setHospitalId(hospitalId);
        p.setUser(cashierUserId);
        p.setRemarks(dto.remarks());
        p.setDepositType(StringUtils.hasText(dto.depositType()) ? dto.depositType() : "Cash");
        p.setAmountReceived(StringUtils.hasText(dto.amountReceived()) ? dto.amountReceived() : "0");
        p.setStatus("unpaid");

        // Auto-set dates
        long nowEpoch = System.currentTimeMillis() / 1000L;
        p.setDate(String.valueOf(nowEpoch));
        p.setDateString(LocalDate.now().format(DATE_STRING_FMT));

        // ── 1. Resolve payment categories for commission lookup ─────────────────
        Map<Integer, PaymentCategory> catMap = buildCategoryMap(dto, hospitalId);

        // ── 2. Calculate subtotal (server-side — never trust frontend) ──────────
        double subtotal = calculateSubtotal(dto);
        p.setAmount(formatMoney(subtotal));

        // ── 3. Apply discount → gross total ────────────────────────────────────
        double grossTotal = applyDiscount(subtotal, dto);
        p.setDiscount(StringUtils.hasText(dto.discount()) ? dto.discount() : "0");
        p.setFlatDiscount(dto.flatDiscount());
        p.setGrossTotal(formatMoney(grossTotal));

        // ── 4. Apply VAT ────────────────────────────────────────────────────────
        double vatPct = parseDouble(dto.vat());
        p.setVat(StringUtils.hasText(dto.vat()) ? dto.vat() : "0");
        if (vatPct > 0) {
            double flatVatAmount = grossTotal * vatPct / 100.0;
            p.setFlatVat(formatMoney(flatVatAmount));
            grossTotal += flatVatAmount;
            p.setGrossTotal(formatMoney(grossTotal));
        }

        // ── 5. Commission split (doctor vs hospital) ────────────────────────────
        double[] split = calculateCommissionSplit(dto, catMap);
        p.setDoctorAmount(formatMoney(split[0]));
        p.setHospitalAmount(formatMoney(split[1]));

        // ── 6. Serialize line items → legacy delimited string ───────────────────
        String categoryAmount = serializeLineItems(dto);
        p.setCategoryAmount(categoryAmount);
        p.setCategoryName(categoryAmount); // legacy: mirrors category_amount

        // ── 7. Denormalize patient/doctor names ─────────────────────────────────
        p.setPatientName(resolvePatientName(dto.patient()));
        p.setPatientPhone(resolvePatientPhone(dto.patient()));
        p.setPatientAddress(resolvePatientAddress(dto.patient()));
        p.setDoctorName(resolveDoctorName(dto.doctor()));

        // ── 8. Save main payment invoice ─────────────────────────────────────────
        Payment saved = paymentRepository.save(p);

        // ── 9. AUDIT FIX Alert 5: patient_deposit double-write ───────────────────
        //
        // CRITICAL: Every payment creation MUST write a patient_deposit record.
        // This is the atomic double-write required by the legacy system.
        // Legacy source: finance/controllers/finance.php lines 358–374
        //
        // The composite key format is: "{paymentId}.gp"
        //   - ".gp" suffix = General Payment (as opposed to ".ot" for OT payments)
        //   - This key is used by the invoice printout to show "Amount Collected"
        //
        // Both writes are wrapped in the same @Transactional — either BOTH succeed
        // or BOTH rollback. Zero partial state.
        PatientDeposit deposit = new PatientDeposit();
        deposit.setDate(String.valueOf(System.currentTimeMillis() / 1000L));
        deposit.setPatient(p.getPatient());
        deposit.setPaymentId(String.valueOf(saved.getId()));
        deposit.setAmountReceivedId(saved.getId() + ".gp");  // CRITICAL: legacy composite key
        deposit.setDepositedAmount(StringUtils.hasText(p.getAmountReceived()) ? p.getAmountReceived() : "0");
        deposit.setDepositType(StringUtils.hasText(p.getDepositType()) ? p.getDepositType() : "Cash");
        deposit.setUser(p.getUser());
        patientDepositRepository.save(deposit);

        return PaymentResponseDto.from(saved);
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<PaymentResponseDto> listAll(String hospitalId, Pageable pageable) {
        return paymentRepository.findByHospitalId(hospitalId, pageable)
                .map(PaymentResponseDto::from);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponseDto> listByPatient(String patientId, String hospitalId) {
        return paymentRepository.findByPatientAndHospitalId(patientId, hospitalId)
                .stream().map(PaymentResponseDto::from).toList();
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponseDto> listByStatus(String status, String hospitalId, Pageable pageable) {
        return paymentRepository.findByStatusAndHospitalId(status, hospitalId, pageable)
                .map(PaymentResponseDto::from);
    }

    @Transactional(readOnly = true)
    public PaymentResponseDto getById(Integer id, String hospitalId) {
        Payment p = paymentRepository.findById(id)
                .filter(pay -> hospitalId.equals(pay.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", id));
        return PaymentResponseDto.from(p);
    }

    // ─── Status Update (unpaid → paid) ────────────────────────────────────────

    @Transactional
    public PaymentResponseDto updateStatus(Integer id, String status, String hospitalId) {
        Payment p = paymentRepository.findById(id)
                .filter(pay -> hospitalId.equals(pay.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", id));
        p.setStatus(status);
        return PaymentResponseDto.from(paymentRepository.save(p));
    }

    /** Update amount received (partial payment). */
    @Transactional
    public PaymentResponseDto updateAmountReceived(Integer id,
                                                    String amountReceived,
                                                    String hospitalId) {
        Payment p = paymentRepository.findById(id)
                .filter(pay -> hospitalId.equals(pay.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", id));
        p.setAmountReceived(amountReceived);
        // Auto-mark paid if amount received >= gross total
        try {
            double received = Double.parseDouble(amountReceived);
            double gross = Double.parseDouble(p.getGrossTotal());
            if (received >= gross) p.setStatus("paid");
        } catch (NumberFormatException ignored) {}
        return PaymentResponseDto.from(paymentRepository.save(p));
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    @Transactional
    public void deletePayment(Integer id, String hospitalId) {
        Payment p = paymentRepository.findById(id)
                .filter(pay -> hospitalId.equals(pay.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", id));
        paymentRepository.delete(p);
    }

    // ─── Private: Financial Math ───────────────────────────────────────────────

    /**
     * Calculates subtotal = Σ (price_per_item × count) for all line items.
     * Server-side — frontend submitted totals are IGNORED.
     */
    private double calculateSubtotal(PaymentRequestDto dto) {
        if (dto.items() == null) return 0.0;
        return dto.items().stream()
            .mapToDouble(item -> parseDouble(item.price()) * (item.count() != null ? item.count() : 1))
            .sum();
    }

    /**
     * Applies discount to subtotal.
     * flatDiscount takes priority over percentage discount.
     */
    private double applyDiscount(double subtotal, PaymentRequestDto dto) {
        if (StringUtils.hasText(dto.flatDiscount()) && parseDouble(dto.flatDiscount()) > 0) {
            return Math.max(0, subtotal - parseDouble(dto.flatDiscount()));
        }
        double discountPct = parseDouble(dto.discount());
        if (discountPct > 0) {
            return subtotal * (1.0 - discountPct / 100.0);
        }
        return subtotal;
    }

    /**
     * Calculates doctor vs hospital commission split.
     * Returns [doctorAmount, hospitalAmount].
     *
     * Per legacy rules:
     *  - 'diagnostic' items: d_commission% → doctor, remainder → hospital
     *  - 'others' items: 0% → doctor, 100% → hospital
     */
    private double[] calculateCommissionSplit(PaymentRequestDto dto,
                                               Map<Integer, PaymentCategory> catMap) {
        double totalDoctor = 0;
        double totalHospital = 0;
        if (dto.items() == null) return new double[]{0, 0};

        for (PaymentRequestDto.PaymentLineItemDto item : dto.items()) {
            double lineTotal = parseDouble(item.price()) *
                               (item.count() != null ? item.count() : 1);
            PaymentCategory cat = catMap.get(item.categoryId());
            int dCommission = (cat != null && cat.getDCommission() != null)
                              ? cat.getDCommission() : 0;
            double docShare = lineTotal * dCommission / 100.0;
            totalDoctor  += docShare;
            totalHospital += (lineTotal - docShare);
        }
        return new double[]{totalDoctor, totalHospital};
    }

    /**
     * Serializes line items → "categoryId*price*type*count,categoryId2*..."
     */
    private String serializeLineItems(PaymentRequestDto dto) {
        if (dto.items() == null || dto.items().isEmpty()) return "";
        return dto.items().stream()
            .map(item -> String.join(
                    Payment.FIELD_SEPARATOR,
                    String.valueOf(item.categoryId()),
                    nvl(item.price()),
                    nvl(item.type()),
                    String.valueOf(item.count() != null ? item.count() : 1)
            ))
            .collect(Collectors.joining(Payment.ITEM_SEPARATOR));
    }

    private Map<Integer, PaymentCategory> buildCategoryMap(PaymentRequestDto dto,
                                                              String hospitalId) {
        if (dto.items() == null) return Map.of();
        List<Integer> ids = dto.items().stream()
                .map(PaymentRequestDto.PaymentLineItemDto::categoryId)
                .toList();
        return paymentCategoryRepository.findAllById(ids)
                .stream()
                .filter(c -> hospitalId.equals(c.getHospitalId()))
                .collect(Collectors.toMap(PaymentCategory::getId, c -> c));
    }

    // ─── Private: Patient/Doctor Info Resolvers ────────────────────────────────

    private String resolvePatientName(String patientId) {
        return resolvePatient(patientId).map(Patient::getName).orElse("");
    }
    private String resolvePatientPhone(String patientId) {
        return resolvePatient(patientId).map(Patient::getPhone).orElse("");
    }
    private String resolvePatientAddress(String patientId) {
        return resolvePatient(patientId).map(Patient::getAddress).orElse("");
    }
    private java.util.Optional<Patient> resolvePatient(String patientId) {
        if (!StringUtils.hasText(patientId)) return java.util.Optional.empty();
        try { return patientRepository.findById(Integer.parseInt(patientId)); }
        catch (NumberFormatException e) { return java.util.Optional.empty(); }
    }

    private String resolveDoctorName(String doctorId) {
        if (!StringUtils.hasText(doctorId) || "0".equals(doctorId)) return "";
        try {
            return doctorRepository.findById(Integer.parseInt(doctorId))
                    .map(Doctor::getName).orElse("");
        } catch (NumberFormatException e) { return ""; }
    }

    // ─── Private: Utilities ───────────────────────────────────────────────────

    private double parseDouble(String s) {
        if (!StringUtils.hasText(s)) return 0.0;
        try { return Double.parseDouble(s.trim()); }
        catch (NumberFormatException e) { return 0.0; }
    }

    /** Format to string — truncate to no decimal for whole numbers. */
    private String formatMoney(double v) {
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.format("%.2f", v);
    }

    private String nvl(String s) { return s != null ? s : ""; }
}
