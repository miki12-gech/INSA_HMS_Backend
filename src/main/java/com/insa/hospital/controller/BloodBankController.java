package com.insa.hospital.controller;

import com.insa.hospital.entity.BloodBank;
import com.insa.hospital.security.JwtContextHolder;
import com.insa.hospital.service.BloodBankService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Blood Bank REST Controller
 * Base URL: /api/blood-bank
 *
 * Manages blood stock levels in the legacy `bankb` table.
 *
 * Roles:
 *  - View: Nurse, Doctor, admin, superadmin
 *  - Update/Create/Delete: admin, superadmin
 */
@RestController
@RequestMapping("/api/blood-bank")
public class BloodBankController {

    private final BloodBankService bloodBankService;

    @Autowired
    public BloodBankController(BloodBankService bloodBankService) {
        this.bloodBankService = bloodBankService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('Nurse', 'Doctor', 'admin', 'superadmin')")
    public ResponseEntity<List<BloodBank>> listAll(Authentication authentication) {
        return ResponseEntity.ok(
                bloodBankService.listAll(JwtContextHolder.getHospitalId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('Nurse', 'Doctor', 'admin', 'superadmin')")
    public ResponseEntity<BloodBank> getById(
            @PathVariable Integer id,
            Authentication authentication) {
        return ResponseEntity.ok(
                bloodBankService.getById(id, JwtContextHolder.getHospitalId()));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<BloodBank> create(
            @RequestBody BloodBank bloodBank,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bloodBankService.create(bloodBank, JwtContextHolder.getHospitalId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<BloodBank> update(
            @PathVariable Integer id,
            @RequestBody BloodBank bloodBank,
            Authentication authentication) {
        return ResponseEntity.ok(
                bloodBankService.update(id, bloodBank, JwtContextHolder.getHospitalId()));
    }

    /**
     * PATCH /api/blood-bank/{id}/status
     * Update only the status (stock level) of a blood group entry.
     * Body: { "status": "5 Bags" }
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<BloodBank> updateStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        String status = body.get("status");
        return ResponseEntity.ok(
                bloodBankService.updateStatus(id, status, JwtContextHolder.getHospitalId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<Void> delete(
            @PathVariable Integer id,
            Authentication authentication) {
        bloodBankService.delete(id, JwtContextHolder.getHospitalId());
        return ResponseEntity.noContent().build();
    }
}
