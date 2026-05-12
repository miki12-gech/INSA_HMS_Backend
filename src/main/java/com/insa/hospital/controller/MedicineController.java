package com.insa.hospital.controller;

import com.insa.hospital.dto.MedicineRequestDto;
import com.insa.hospital.dto.MedicineResponseDto;
import com.insa.hospital.entity.MedicineCategory;
import com.insa.hospital.repository.MedicineCategoryRepository;
import com.insa.hospital.security.JwtContextHolder;
import com.insa.hospital.service.MedicineService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller — Medicine (Pharmacy Stock) endpoints.
 *
 * Base URL: /api/medicines
 *
 * Also exposes:
 *   GET /api/medicines/all    → unpaged list for prescription dropdowns
 *   GET /api/medicines/categories → medicine_category list for dropdowns
 */
@RestController
@RequestMapping("/api/medicines")
public class MedicineController {

    private final MedicineService medicineService;
    private final MedicineCategoryRepository medicineCategoryRepository;

    @Autowired
    public MedicineController(MedicineService medicineService,
                              MedicineCategoryRepository medicineCategoryRepository) {
        this.medicineService = medicineService;
        this.medicineCategoryRepository = medicineCategoryRepository;
    }

    // ─── Dropdown: Unpaged List ───────────────────────────────────────────────

    /**
     * GET /api/medicines/all
     * Returns all medicines for hospital (no pagination) — for prescription dropdowns.
     * All authenticated roles can access.
     */
    @GetMapping("/all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MedicineResponseDto>> listAll() {
        return ResponseEntity.ok(medicineService.listAllMedicines(JwtContextHolder.getHospitalId()));
    }

    // ─── Dropdown: Medicine Categories ────────────────────────────────────────

    /**
     * GET /api/medicines/categories
     * Returns medicine_category list for hospital (for form dropdowns).
     */
    @GetMapping("/categories")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MedicineCategory>> listCategories() {
        return ResponseEntity.ok(
            medicineCategoryRepository.findByHospitalId(JwtContextHolder.getHospitalId()));
    }

    // ─── Paginated List ───────────────────────────────────────────────────────

    /**
     * GET /api/medicines?page=0&size=20&search=para
     * Paginated list with optional name/generic search.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<MedicineResponseDto>> listMedicines(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    String search) {
        String hospitalId = JwtContextHolder.getHospitalId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(medicineService.searchMedicines(hospitalId, search, pageable));
        }
        return ResponseEntity.ok(medicineService.listMedicines(hospitalId, pageable));
    }

    // ─── Stock Alerts & Valuation ─────────────────────────────────────────────

    /** GET /api/medicines/alerts?page=0&size=20 */
    @GetMapping("/alerts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<MedicineResponseDto>> listStockAlerts(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        return ResponseEntity.ok(medicineService.listStockAlerts(JwtContextHolder.getHospitalId(), pageable));
    }

    /** GET /api/medicines/total-value */
    @GetMapping("/total-value")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin', 'Pharmacist', 'Accountant')")
    public ResponseEntity<Double> getTotalValue() {
        return ResponseEntity.ok(medicineService.getTotalStockPrice(JwtContextHolder.getHospitalId()));
    }

    // ─── Get Single ───────────────────────────────────────────────────────────

    /** GET /api/medicines/{id} */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MedicineResponseDto> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(medicineService.getById(id, JwtContextHolder.getHospitalId()));
    }

    // ─── Create ───────────────────────────────────────────────────────────────

    /** POST /api/medicines */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin', 'Pharmacist')")
    public ResponseEntity<MedicineResponseDto> createMedicine(
            @Valid @RequestBody MedicineRequestDto dto) {
        String hospitalId = JwtContextHolder.getHospitalId();
        Integer userId = null;
        try { userId = Integer.parseInt(JwtContextHolder.getUserId()); } catch (Exception ignored) {}
        return ResponseEntity.ok(medicineService.createMedicine(dto, hospitalId, userId));
    }

    // ─── Update & Load ────────────────────────────────────────────────────────

    /** POST /api/medicines/{id}/load?qty=50 */
    @PostMapping("/{id}/load")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin', 'Pharmacist')")
    public ResponseEntity<Void> loadMedicine(
            @PathVariable Integer id,
            @RequestParam Integer qty) {
        medicineService.loadMedicine(id, qty, JwtContextHolder.getHospitalId());
        return ResponseEntity.ok().build();
    }

    /** PUT /api/medicines/{id} */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin', 'Pharmacist')")
    public ResponseEntity<MedicineResponseDto> updateMedicine(
            @PathVariable Integer id,
            @Valid @RequestBody MedicineRequestDto dto) {
        return ResponseEntity.ok(
            medicineService.updateMedicine(id, dto, JwtContextHolder.getHospitalId()));
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    /** DELETE /api/medicines/{id} */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<Void> deleteMedicine(@PathVariable Integer id) {
        medicineService.deleteMedicine(id, JwtContextHolder.getHospitalId());
        return ResponseEntity.noContent().build();
    }
}
