package com.insa.hospital.controller;

import com.insa.hospital.dto.*;
import com.insa.hospital.security.JwtContextHolder;
import com.insa.hospital.service.ReferralService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Referral REST Controller
 * Base URL: /api/referrals
 *
 * Implements the Corporate Referral & External Billing workflow for INSA.
 *
 * Endpoints:
 *   POST   /request          → Doctor/Employee creates a referral (PENDING).
 *   GET    /pending           → Admin views the approval queue.
 *   PUT    /{id}/approve      → Admin approves or rejects a referral.
 *   PUT    /{id}/settle-bill  → Finance/Admin records external bill (COMPLETED).
 *   GET    /my-requests       → Employee views their own referral history.
 *   GET    /all               → Admin views all referrals (any status).
 *   GET    /{id}              → Get a single referral by ID.
 *   GET    /patient/{pid}     → Get all referrals for a specific patient.
 *
 * All endpoints require a valid JWT Bearer token.
 * Role checks use @PreAuthorize with legacy group names (e.g. 'Doctor', 'admin').
 * Multi-tenancy is enforced via hospitalId from JwtContextHolder.
 */
@RestController
@RequestMapping("/api/referrals")
public class ReferralController {

    private final ReferralService referralService;

    @Autowired
    public ReferralController(ReferralService referralService) {
        this.referralService = referralService;
    }

    // ─── POST /api/referrals/request ─────────────────────────────────────────
    // Doctor or Employee creates a new referral request.
    // Status is set to PENDING — awaits Admin approval.

    @PostMapping("/request")
    @PreAuthorize("hasAnyAuthority('Doctor', 'admin', 'superadmin', 'Employee')")
    public ResponseEntity<ReferralResponseDto> requestReferral(
            @Valid @RequestBody ReferralCreationDto dto,
            Authentication authentication) {

        String hospitalId = JwtContextHolder.getHospitalId();
        String userId = JwtContextHolder.getUserId();

        ReferralResponseDto created = referralService.requestReferral(
                dto, hospitalId, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ─── GET /api/referrals/pending ──────────────────────────────────────────
    // Admin views all PENDING referrals (approval queue).

    @GetMapping("/pending")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<List<ReferralResponseDto>> getPendingReferrals(
            Authentication authentication) {

        String hospitalId = JwtContextHolder.getHospitalId();
        return ResponseEntity.ok(referralService.getPendingReferrals(hospitalId));
    }

    // ─── PUT /api/referrals/{id}/approve ─────────────────────────────────────
    // Admin approves or rejects a PENDING referral.

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<ReferralResponseDto> approveReferral(
            @PathVariable Long id,
            @Valid @RequestBody ReferralApprovalDto dto,
            Authentication authentication) {

        String hospitalId = JwtContextHolder.getHospitalId();
        String userId = JwtContextHolder.getUserId();
        ReferralResponseDto updated = referralService.approveReferral(
                id, dto, hospitalId, userId);

        return ResponseEntity.ok(updated);
    }

    // ─── PUT /api/referrals/{id}/settle-bill ─────────────────────────────────
    // Finance or Admin records the external hospital bill → COMPLETED.

    @PutMapping("/{id}/settle-bill")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin', 'Finance')")
    public ResponseEntity<ReferralResponseDto> settleExternalBill(
            @PathVariable Long id,
            @Valid @RequestBody ReferralBillingDto dto,
            Authentication authentication) {

        String hospitalId = JwtContextHolder.getHospitalId();
        ReferralResponseDto updated = referralService.settleExternalBill(
                id, dto, hospitalId);

        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/letter-management-upload")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<ReferralResponseDto> sendToLetterManagement(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "note", required = false) String note,
            Authentication authentication) throws IOException {

        String hospitalId = JwtContextHolder.getHospitalId();
        String userId = JwtContextHolder.getUserId();
        ReferralResponseDto updated = referralService.sendToLetterManagement(
                id, file, note, hospitalId, userId);

        return ResponseEntity.ok(updated);
    }

    // ─── GET /api/referrals/my-requests ──────────────────────────────────────
    // Employee views their own referral requests.

    @GetMapping("/my-requests")
    @PreAuthorize("hasAnyAuthority('Employee', 'Doctor', 'admin', 'superadmin')")
    public ResponseEntity<List<ReferralResponseDto>> getMyRequests(
            Authentication authentication) {

        String hospitalId = JwtContextHolder.getHospitalId();
        String userId = JwtContextHolder.getUserId();

        return ResponseEntity.ok(
                referralService.getMyRequests(userId, hospitalId));
    }

    // ─── GET /api/referrals/all ──────────────────────────────────────────────
    // Admin overview — all referrals regardless of status.

    @GetMapping("/all")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<List<ReferralResponseDto>> getAllReferrals(
            Authentication authentication) {

        String hospitalId = JwtContextHolder.getHospitalId();
        return ResponseEntity.ok(referralService.getAllReferrals(hospitalId));
    }

    // ─── GET /api/referrals/{id} ─────────────────────────────────────────────
    // Get a single referral by ID.

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReferralResponseDto> getReferralById(
            @PathVariable Long id,
            Authentication authentication) {

        String hospitalId = JwtContextHolder.getHospitalId();
        return ResponseEntity.ok(
                referralService.getReferralById(id, hospitalId));
    }

    // ─── GET /api/referrals/patient/{patientId} ──────────────────────────────
    // Get all referrals for a specific patient.

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyAuthority('Doctor', 'admin', 'superadmin', 'Employee')")
    public ResponseEntity<List<ReferralResponseDto>> getPatientReferrals(
            @PathVariable String patientId,
            Authentication authentication) {

        String hospitalId = JwtContextHolder.getHospitalId();
        return ResponseEntity.ok(
                referralService.getPatientReferrals(patientId, hospitalId));
    }
}
