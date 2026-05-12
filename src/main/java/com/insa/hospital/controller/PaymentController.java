package com.insa.hospital.controller;

import com.insa.hospital.dto.PaymentRequestDto;
import com.insa.hospital.dto.PaymentResponseDto;
import com.insa.hospital.dto.PaymentCategoryUpsertDto;
import com.insa.hospital.dto.ServiceChargeBulkCreateRequestDto;
import com.insa.hospital.dto.ServiceChargeResponseDto;
import com.insa.hospital.dto.ServiceChargeSummaryResponseDto;
import com.insa.hospital.entity.PaymentCategory;
import com.insa.hospital.security.JwtContextHolder;
import com.insa.hospital.service.BillingCatalogService;
import com.insa.hospital.service.PaymentService;
import com.insa.hospital.service.ServiceChargeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller — Payment / Invoice endpoints.
 *
 * Base URL: /api/payments
 *
 * Key endpoints:
 *   POST   /api/payments                       → Create invoice (Receptionist/Accountant)
 *   GET    /api/payments                        → Paginated list (?status=unpaid)
 *   GET    /api/payments/{id}                   → Get single invoice
 *   GET    /api/payments/patient/{patientId}    → Patient billing history
 *   PATCH  /api/payments/{id}/status            → Mark paid/unpaid
 *   PATCH  /api/payments/{id}/collect           → Record amount received
 *   GET    /api/payments/categories             → Fee catalog for billing form
 *   DELETE /api/payments/{id}                   → Admin only
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final BillingCatalogService billingCatalogService;
    private final ServiceChargeService serviceChargeService;

    @Autowired
    public PaymentController(PaymentService paymentService,
                             BillingCatalogService billingCatalogService,
                             ServiceChargeService serviceChargeService) {
        this.paymentService = paymentService;
        this.billingCatalogService = billingCatalogService;
        this.serviceChargeService = serviceChargeService;
    }

    // ─── Fee Catalogue Dropdown ───────────────────────────────────────────────

    /**
     * GET /api/payments/categories
     * Returns all payment_category items for the hospital.
     * Used by the billing form to populate the line-item dropdown.
     */
    @GetMapping("/categories")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PaymentCategory>> listCategories() {
        return ResponseEntity.ok(billingCatalogService.listCatalog(JwtContextHolder.getHospitalId(), false));
    }

    @GetMapping("/catalog")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PaymentCategory>> listServiceCatalog(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(
                billingCatalogService.listCatalog(JwtContextHolder.getHospitalId(), activeOnly));
    }

    @PostMapping("/catalog")
    @PreAuthorize("hasAnyAuthority('Accountant', 'admin', 'superadmin')")
    public ResponseEntity<PaymentCategory> createServiceCatalogItem(
            @Valid @RequestBody PaymentCategoryUpsertDto dto) {
        return ResponseEntity.ok(
                billingCatalogService.createCategory(dto, JwtContextHolder.getHospitalId()));
    }

    @PutMapping("/catalog/{id}")
    @PreAuthorize("hasAnyAuthority('Accountant', 'admin', 'superadmin')")
    public ResponseEntity<PaymentCategory> updateServiceCatalogItem(
            @PathVariable Integer id,
            @Valid @RequestBody PaymentCategoryUpsertDto dto) {
        return ResponseEntity.ok(
                billingCatalogService.updateCategory(id, dto, JwtContextHolder.getHospitalId()));
    }

    @PatchMapping("/catalog/{id}/status")
    @PreAuthorize("hasAnyAuthority('Accountant', 'admin', 'superadmin')")
    public ResponseEntity<PaymentCategory> updateCatalogStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> body) {
        Object value = body.get("active");
        boolean active = !(value instanceof Boolean booleanValue) || booleanValue;
        return ResponseEntity.ok(
                billingCatalogService.updateCategoryStatus(id, active, JwtContextHolder.getHospitalId()));
    }

    @DeleteMapping("/catalog/{id}")
    @PreAuthorize("hasAnyAuthority('Accountant', 'admin', 'superadmin')")
    public ResponseEntity<Void> deleteServiceCatalogItem(@PathVariable Integer id) {
        billingCatalogService.deleteCategory(id, JwtContextHolder.getHospitalId());
        return ResponseEntity.noContent().build();
    }

    // ─── Create Invoice ───────────────────────────────────────────────────────

    /**
     * POST /api/payments
     * Creates a new invoice / bill. Totals calculated server-side.
     *
     * Example body:
     * {
     *   "patient": "39",
     *   "doctor": "149",
     *   "depositType": "Cash",
     *   "discount": "10",
     *   "items": [
     *     { "categoryId": 88, "price": "350", "type": "diagnostic", "count": 1 },
     *     { "categoryId": 36, "price": "450", "type": "diagnostic", "count": 1 }
     *   ]
     * }
     *
     * Server calculates: subtotal → apply discount → gross_total → split commissions
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('Receptionist', 'Accountant', 'admin', 'superadmin')")
    public ResponseEntity<PaymentResponseDto> createPayment(
            @Valid @RequestBody PaymentRequestDto dto) {
        String hospitalId = JwtContextHolder.getHospitalId();
        String userId     = JwtContextHolder.getUserId();
        return ResponseEntity.ok(paymentService.createPayment(dto, hospitalId, userId));
    }

    // ─── List All ─────────────────────────────────────────────────────────────

    /**
     * GET /api/payments?page=0&size=20&status=unpaid
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<PaymentResponseDto>> listAll(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    String status) {
        String hospitalId = JwtContextHolder.getHospitalId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        if (status != null && !status.isBlank()) {
            return ResponseEntity.ok(paymentService.listByStatus(status, hospitalId, pageable));
        }
        return ResponseEntity.ok(paymentService.listAll(hospitalId, pageable));
    }

    // ─── Patient Billing History ───────────────────────────────────────────────

    /**
     * GET /api/payments/patient/{patientId}
     * Full billing history for a patient (for patient profile).
     */
    @GetMapping("/patient/{patientId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PaymentResponseDto>> listByPatient(
            @PathVariable String patientId) {
        return ResponseEntity.ok(
            paymentService.listByPatient(patientId, JwtContextHolder.getHospitalId()));
    }

    // ─── Get Single ───────────────────────────────────────────────────────────

    /** GET /api/payments/{id} */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponseDto> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(
            paymentService.getById(id, JwtContextHolder.getHospitalId()));
    }

    // ─── Status Update: unpaid → paid ─────────────────────────────────────────

    /**
     * PATCH /api/payments/{id}/status
     * Body: { "status": "paid" }
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('Receptionist', 'Accountant', 'admin', 'superadmin')")
    public ResponseEntity<PaymentResponseDto> updateStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body) {
        String status = body.getOrDefault("status", "paid");
        return ResponseEntity.ok(
            paymentService.updateStatus(id, status, JwtContextHolder.getHospitalId()));
    }

    // ─── Collect Payment ──────────────────────────────────────────────────────

    /**
     * PATCH /api/payments/{id}/collect
     * Records the amount received (partial or full payment).
     * Auto-marks status='paid' if amountReceived >= grossTotal.
     * Body: { "amountReceived": "1050" }
     */
    @PatchMapping("/{id}/collect")
    @PreAuthorize("hasAnyAuthority('Receptionist', 'Accountant', 'admin', 'superadmin')")
    public ResponseEntity<PaymentResponseDto> collectPayment(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body) {
        String amount = body.getOrDefault("amountReceived", "0");
        return ResponseEntity.ok(
            paymentService.updateAmountReceived(id, amount, JwtContextHolder.getHospitalId()));
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    /** DELETE /api/payments/{id} */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<Void> deletePayment(@PathVariable Integer id) {
        paymentService.deletePayment(id, JwtContextHolder.getHospitalId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/service-charges")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ServiceChargeResponseDto>> listServiceCharges(
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) String visitId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        return ResponseEntity.ok(serviceChargeService.listCharges(
                JwtContextHolder.getHospitalId(),
                patientId,
                visitId,
                status,
                dateFrom,
                dateTo
        ));
    }

    @PostMapping("/service-charges")
    @PreAuthorize("hasAnyAuthority('Doctor', 'Nurse', 'Receptionist', 'Accountant', 'admin', 'superadmin')")
    public ResponseEntity<List<ServiceChargeResponseDto>> createServiceCharges(
            @Valid @RequestBody ServiceChargeBulkCreateRequestDto dto,
            Authentication authentication) {
        return ResponseEntity.ok(serviceChargeService.createCharges(
                dto,
                JwtContextHolder.getHospitalId(),
                JwtContextHolder.getUserId(),
                authentication != null ? authentication.getName() : null,
                JwtContextHolder.getRole()
        ));
    }

    @PatchMapping("/service-charges/{id}/collect")
    @PreAuthorize("hasAnyAuthority('Receptionist', 'Accountant', 'admin', 'superadmin')")
    public ResponseEntity<ServiceChargeResponseDto> collectServiceCharge(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(serviceChargeService.collectPayment(
                id,
                body.getOrDefault("paidAmount", "0"),
                JwtContextHolder.getHospitalId()
        ));
    }

    @PatchMapping("/service-charges/{id}/status")
    @PreAuthorize("hasAnyAuthority('Doctor', 'Nurse', 'Receptionist', 'Accountant', 'admin', 'superadmin')")
    public ResponseEntity<ServiceChargeResponseDto> updateServiceChargeStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(serviceChargeService.updateStatus(
                id,
                body.getOrDefault("status", "PENDING"),
                JwtContextHolder.getHospitalId()
        ));
    }

    @GetMapping("/service-report")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServiceChargeSummaryResponseDto> getServiceReport(
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        return ResponseEntity.ok(
                serviceChargeService.buildSummary(JwtContextHolder.getHospitalId(), dateFrom, dateTo));
    }
}
