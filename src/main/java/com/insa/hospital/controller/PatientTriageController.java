package com.insa.hospital.controller;

import com.insa.hospital.dto.PatientTriageRequestDto;
import com.insa.hospital.dto.PatientTriageResponseDto;
import com.insa.hospital.security.JwtContextHolder;
import com.insa.hospital.service.PatientTriageService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Patient Triage (Vitals) REST Controller
 * Base URL: /api/triage
 *
 * Roles:
 *  - Record/Update: Nurse, Doctor, admin, superadmin
 *  - View: all authenticated roles
 *  - Delete: admin, superadmin
 */
@RestController
@RequestMapping("/api/triage")
public class PatientTriageController {

    private final PatientTriageService triageService;

    @Autowired
    public PatientTriageController(PatientTriageService triageService) {
        this.triageService = triageService;
    }

    // ─── POST /api/triage ─────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyAuthority('Nurse', 'Doctor', 'admin', 'superadmin')")
    public ResponseEntity<PatientTriageResponseDto> recordTriage(
            @Valid @RequestBody PatientTriageRequestDto dto,
            Authentication authentication) {

        String hospitalId = JwtContextHolder.getHospitalId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(triageService.recordTriage(dto, hospitalId));
    }

    // ─── GET /api/triage ──────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<PatientTriageResponseDto>> listTriage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Authentication authentication) {

        String hospitalId = JwtContextHolder.getHospitalId();
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(triageService.listTriage(name, startDate, endDate, hospitalId, pageable));
    }

    // ─── GET /api/triage/patient/{patientId} ──────────────────────────────────

    /** All triage records for a specific patient (vitals history). */
    @GetMapping("/patient/{patientId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PatientTriageResponseDto>> getByPatient(
            @PathVariable String patientId,
            Authentication authentication) {

        String hospitalId = JwtContextHolder.getHospitalId();
        return ResponseEntity.ok(triageService.listByPatient(patientId, hospitalId));
    }

    // ─── GET /api/triage/patient/{patientId}/latest ───────────────────────────

    /** Most recent triage entry — for quick vitals display on patient profile. */
    @GetMapping("/patient/{patientId}/latest")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PatientTriageResponseDto> getLatestByPatient(
            @PathVariable String patientId,
            Authentication authentication) {

        String hospitalId = JwtContextHolder.getHospitalId();
        return ResponseEntity.ok(triageService.getLatestByPatient(patientId, hospitalId));
    }

    // ─── GET /api/triage/{id} ─────────────────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PatientTriageResponseDto> getById(
            @PathVariable Integer id,
            Authentication authentication) {

        String hospitalId = JwtContextHolder.getHospitalId();
        return ResponseEntity.ok(triageService.getTriageById(id, hospitalId));
    }

    // ─── PUT /api/triage/{id} ─────────────────────────────────────────────────

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('Nurse', 'Doctor', 'admin', 'superadmin')")
    public ResponseEntity<PatientTriageResponseDto> updateTriage(
            @PathVariable Integer id,
            @Valid @RequestBody PatientTriageRequestDto dto,
            Authentication authentication) {

        String hospitalId = JwtContextHolder.getHospitalId();
        return ResponseEntity.ok(triageService.updateTriage(id, dto, hospitalId));
    }

    // ─── DELETE /api/triage/{id} ──────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<Void> deleteTriage(
            @PathVariable Integer id,
            Authentication authentication) {

        triageService.deleteTriage(id, JwtContextHolder.getHospitalId());
        return ResponseEntity.noContent().build();
    }
}
