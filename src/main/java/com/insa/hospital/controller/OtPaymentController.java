package com.insa.hospital.controller;

import com.insa.hospital.entity.OtPayment;
import com.insa.hospital.security.JwtContextHolder;
import com.insa.hospital.service.OtPaymentService;
import com.insa.hospital.service.OtPaymentService.OtPaymentRequestDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller — Operating Theatre (OT) Payment endpoints.
 *
 * Base URL: /api/payments/ot
 *
 * RBAC (from legacy finance.php __construct line 14):
 *   Controller access: admin, Accountant, Receptionist, Doctor, Nurse
 *   Write (create/edit/delete): admin, Accountant, Receptionist
 *   Read: same + Doctor, Nurse
 *   Delete: admin only
 *
 * ══════════════════════════════════════════════════════════════
 *  Endpoints:
 *
 *  POST   /api/payments/ot                     → Create OT invoice
 *  GET    /api/payments/ot                     → Paginated list (?status=unpaid)
 *  GET    /api/payments/ot/{id}                → Get single OT invoice
 *  GET    /api/payments/ot/patient/{patientId} → Patient OT billing history
 *  PATCH  /api/payments/ot/{id}/status         → Mark paid/unpaid
 *  DELETE /api/payments/ot/{id}                → Admin only
 *
 * NOTE: The server calculates all fees from the submitted components.
 * Frontend MUST NOT submit gross_total, doctor_fees, or hospital_fees —
 * they are calculated server-side and will be ignored if submitted.
 * ══════════════════════════════════════════════════════════════
 */
@RestController
@RequestMapping("/api/payments/ot")
public class OtPaymentController {

    private final OtPaymentService otPaymentService;

    @Autowired
    public OtPaymentController(OtPaymentService otPaymentService) {
        this.otPaymentService = otPaymentService;
    }

    // ─── Create OT Invoice ────────────────────────────────────────────────────

    /**
     * POST /api/payments/ot
     *
     * Creates an OT invoice. Server calculates:
     *   doctor_fees, hospital_fees, gross_total (with discount absorbed by hospital side only).
     *
     * Example body:
     * {
     *   "patient": "39",
     *   "doctorCs": "149",
     *   "doctorAs1": "152",
     *   "doctorAs2": "",
     *   "doctorAnaes": "155",
     *   "noo": "Appendectomy",
     *   "csf":     "3000",
     *   "asf1":    "1500",
     *   "asf2":    "0",
     *   "anaesF":  "1200",
     *   "otCharge":"5000",
     *   "cabRent": "500",
     *   "seatRent":"700",
     *   "others":  "200",
     *   "discount": "10",
     *   "discountType": "percentage",
     *   "vat": "0",
     *   "depositType": "Cash",
     *   "amountReceived": "5000"
     * }
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin', 'Accountant', 'Receptionist')")
    public ResponseEntity<OtPayment> createOtPayment(@RequestBody OtPaymentRequestDto dto) {
        String hospitalId = JwtContextHolder.getHospitalId();
        String userId     = JwtContextHolder.getUserId();
        OtPayment saved = otPaymentService.createOtPayment(dto, hospitalId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ─── List All ─────────────────────────────────────────────────────────────

    /**
     * GET /api/payments/ot?page=0&size=20
     * GET /api/payments/ot?status=unpaid
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin', 'Accountant', 'Receptionist', 'Doctor', 'Nurse')")
    public ResponseEntity<Page<OtPayment>> listAll(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        String hospitalId = JwtContextHolder.getHospitalId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<OtPayment> result = (status != null && !status.isBlank())
                ? otPaymentService.listByStatus(status, hospitalId, pageable)
                : otPaymentService.listAll(hospitalId, pageable);
        return ResponseEntity.ok(result);
    }

    // ─── Patient OT History ───────────────────────────────────────────────────

    /** GET /api/payments/ot/patient/{patientId} */
    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin', 'Accountant', 'Receptionist', 'Doctor', 'Nurse')")
    public ResponseEntity<List<OtPayment>> listByPatient(@PathVariable String patientId) {
        return ResponseEntity.ok(
            otPaymentService.listByPatient(patientId, JwtContextHolder.getHospitalId()));
    }

    // ─── Get Single ───────────────────────────────────────────────────────────

    /** GET /api/payments/ot/{id} */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin', 'Accountant', 'Receptionist', 'Doctor', 'Nurse')")
    public ResponseEntity<OtPayment> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(
            otPaymentService.getById(id, JwtContextHolder.getHospitalId()));
    }

    // ─── Status Update ────────────────────────────────────────────────────────

    /**
     * PATCH /api/payments/ot/{id}/status
     * Body: { "status": "paid" }
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin', 'Accountant', 'Receptionist')")
    public ResponseEntity<OtPayment> updateStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body) {
        String status = body.getOrDefault("status", "paid");
        return ResponseEntity.ok(
            otPaymentService.updateStatus(id, status, JwtContextHolder.getHospitalId()));
    }

    // ─── Full Update ──────────────────────────────────────────────────────────

    /**
     * PUT /api/payments/ot/{id}
     *
     * Full update of an OT invoice. Recalculates all fees server-side
     * using the same legacy formula as POST. Also syncs the linked
     * PatientDeposit row (amountReceivedId = "{id}.ot").
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin', 'Accountant', 'Receptionist')")
    public ResponseEntity<OtPayment> updateOtPayment(
            @PathVariable Integer id,
            @RequestBody OtPaymentRequestDto dto) {
        String hospitalId = JwtContextHolder.getHospitalId();
        return ResponseEntity.ok(otPaymentService.updateOtPayment(id, dto, hospitalId));
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    /** DELETE /api/payments/ot/{id} — Admin only */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<Void> deleteOtPayment(@PathVariable Integer id) {
        otPaymentService.deleteOtPayment(id, JwtContextHolder.getHospitalId());
        return ResponseEntity.noContent().build();
    }
}
