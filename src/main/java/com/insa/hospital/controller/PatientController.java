package com.insa.hospital.controller;

import com.insa.hospital.dto.PatientRequestDto;
import com.insa.hospital.dto.PatientResponseDto;
import com.insa.hospital.security.JwtContextHolder;
import com.insa.hospital.service.PatientService;
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
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * Patient REST Controller
 * Base URL: /api/patients
 *
 * All endpoints require a valid JWT Bearer token (enforced by SecurityConfig).
 * Multi-tenancy is enforced via hospitalId and userId extracted from the JWT.
 *
 * Role-based access:
 *  - Register/Update/Delete : Receptionist, admin, superadmin
 *  - View (GET)             : All authenticated roles
 *    (enforced via @PreAuthorize using legacy group names stored in ROLE_ prefix)
 */
@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;

    @Autowired
    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }


    // ─── POST /api/patients/register ─────────────────────────────────────────

    /**
     * Register a new patient.
     * FEATURE: If 'age' is provided without 'birthdate', the service
     *          auto-calculates the birthdate (01-01-YYYY) — receptionist workflow.
     */
    @PostMapping("/register")
    @PreAuthorize("hasAnyAuthority('Receptionist', 'admin', 'superadmin', 'Doctor')")
    public ResponseEntity<PatientResponseDto> registerPatient(
            @Valid @RequestBody PatientRequestDto dto,
            Authentication authentication) {

        String hospitalId = JwtContextHolder.getHospitalId();
        String ionUserId  = JwtContextHolder.getUserId();

        PatientResponseDto created = patientService.registerPatient(dto, hospitalId, ionUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ─── GET /api/patients ────────────────────────────────────────────────────

    @GetMapping("/all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.List<PatientResponseDto>> getAllPatients(Authentication authentication) {
        String hospitalId = JwtContextHolder.getHospitalId();
        return ResponseEntity.ok(patientService.getAllPatients(hospitalId));
    }

    /**
     * List all patients for the caller's hospital (paginated).
     * Supports optional ?search= query parameter for name/phone/patientId lookup.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<PatientResponseDto>> listPatients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String search,
            Authentication authentication) {

        String hospitalId = JwtContextHolder.getHospitalId();

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<PatientResponseDto> result = StringUtils.hasText(search)
                ? patientService.searchPatients(hospitalId, search, pageable)
                : patientService.listPatients(hospitalId, pageable);

        return ResponseEntity.ok(result);
    }

    // ─── GET /api/patients/{id} ───────────────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PatientResponseDto> getPatient(
            @PathVariable Integer id,
            Authentication authentication) {

        String hospitalId = JwtContextHolder.getHospitalId();
        return ResponseEntity.ok(patientService.getPatientById(id, hospitalId));
    }

    // ─── PUT /api/patients/{id} ───────────────────────────────────────────────

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('Receptionist', 'admin', 'superadmin', 'Doctor')")
    public ResponseEntity<PatientResponseDto> updatePatient(
            @PathVariable Integer id,
            @Valid @RequestBody PatientRequestDto dto,
            Authentication authentication) {

        String hospitalId = JwtContextHolder.getHospitalId();
        return ResponseEntity.ok(patientService.updatePatient(id, dto, hospitalId));
    }

    // ─── DELETE /api/patients/{id} ────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<Void> deletePatient(
            @PathVariable Integer id,
            Authentication authentication) {

        String hospitalId = JwtContextHolder.getHospitalId();
        patientService.deletePatient(id, hospitalId);
        return ResponseEntity.noContent().build();
    }
}
