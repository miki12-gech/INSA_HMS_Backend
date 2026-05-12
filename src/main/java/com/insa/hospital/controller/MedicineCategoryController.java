package com.insa.hospital.controller;

import com.insa.hospital.entity.MedicineCategory;
import com.insa.hospital.entity.MedicineCategory1;
import com.insa.hospital.security.JwtContextHolder;
import com.insa.hospital.service.MedicineCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medicine-categories")
public class MedicineCategoryController {

    private final MedicineCategoryService categoryService;

    @Autowired
    public MedicineCategoryController(MedicineCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // ─── Primary Categories (medicine_category) ──────────────────────────────

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MedicineCategory>> listPrimary() {
        return ResponseEntity.ok(categoryService.listPrimaryCategories(JwtContextHolder.getHospitalId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MedicineCategory> getPrimary(@PathVariable Integer id) {
        return ResponseEntity.ok(categoryService.getPrimaryCategory(id, JwtContextHolder.getHospitalId()));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin', 'Pharmacist')")
    public ResponseEntity<MedicineCategory> createPrimary(@RequestBody MedicineCategory category) {
        return ResponseEntity.ok(categoryService.createPrimaryCategory(category, JwtContextHolder.getHospitalId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin', 'Pharmacist')")
    public ResponseEntity<MedicineCategory> updatePrimary(@PathVariable Integer id, @RequestBody MedicineCategory category) {
        return ResponseEntity.ok(categoryService.updatePrimaryCategory(id, category, JwtContextHolder.getHospitalId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin', 'Pharmacist')")
    public ResponseEntity<Void> deletePrimary(@PathVariable Integer id) {
        categoryService.deletePrimaryCategory(id, JwtContextHolder.getHospitalId());
        return ResponseEntity.noContent().build();
    }

    // ─── Secondary Categories (medicine_category1) ───────────────────────────

    @GetMapping("/sub")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MedicineCategory1>> listSecondary() {
        return ResponseEntity.ok(categoryService.listSecondaryCategories(JwtContextHolder.getHospitalId()));
    }

    @GetMapping("/sub/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MedicineCategory1> getSecondary(@PathVariable Integer id) {
        return ResponseEntity.ok(categoryService.getSecondaryCategory(id, JwtContextHolder.getHospitalId()));
    }

    @PostMapping("/sub")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin', 'Pharmacist')")
    public ResponseEntity<MedicineCategory1> createSecondary(@RequestBody MedicineCategory1 category) {
        return ResponseEntity.ok(categoryService.createSecondaryCategory(category, JwtContextHolder.getHospitalId()));
    }

    @PutMapping("/sub/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin', 'Pharmacist')")
    public ResponseEntity<MedicineCategory1> updateSecondary(@PathVariable Integer id, @RequestBody MedicineCategory1 category) {
        return ResponseEntity.ok(categoryService.updateSecondaryCategory(id, category, JwtContextHolder.getHospitalId()));
    }

    @DeleteMapping("/sub/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin', 'Pharmacist')")
    public ResponseEntity<Void> deleteSecondary(@PathVariable Integer id) {
        categoryService.deleteSecondaryCategory(id, JwtContextHolder.getHospitalId());
        return ResponseEntity.noContent().build();
    }
}
