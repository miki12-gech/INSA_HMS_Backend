package com.insa.hospital.controller;

import com.insa.hospital.entity.MedicalReport;
import com.insa.hospital.security.JwtContextHolder;
import com.insa.hospital.service.MedicalReportService;
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
 * Medical Report REST Controller
 * Base URL: /api/reports
 *
 * Maps to the legacy `report` table — supports birth, operation, expire, etc.
 *
 * Roles:
 *  - View: Doctor, Nurse, admin, superadmin
 *  - Create/Update: Doctor, admin, superadmin
 *  - Delete: admin, superadmin
 */
@RestController
@RequestMapping("/api/reports")
public class MedicalReportController {

    private final MedicalReportService reportService;

    @Autowired
    public MedicalReportController(MedicalReportService reportService) {
        this.reportService = reportService;
    }

    // ─── POST /api/reports ────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyAuthority('Doctor', 'admin', 'superadmin')")
    public ResponseEntity<MedicalReport> create(
            @RequestBody MedicalReport report,
            Authentication authentication) {
        String hospitalId = JwtContextHolder.getHospitalId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportService.create(report, hospitalId));
    }

    // ─── GET /api/reports ─────────────────────────────────────────────────────

    /**
     * List reports with optional filters.
     * ?report_type=birth|operation|expire, ?patient=id, ?doctor=id
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('Doctor', 'Nurse', 'admin', 'superadmin')")
    public ResponseEntity<Page<MedicalReport>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String report_type,
            @RequestParam(required = false) String patient,
            @RequestParam(required = false) String doctor,
            Authentication authentication) {

        String hospitalId = JwtContextHolder.getHospitalId();
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<MedicalReport> result;
        if (StringUtils.hasText(report_type)) {
            result = reportService.listByType(report_type, hospitalId, pageable);
        } else if (StringUtils.hasText(patient)) {
            result = reportService.listByPatient(patient, hospitalId, pageable);
        } else if (StringUtils.hasText(doctor)) {
            result = reportService.listByDoctor(doctor, hospitalId, pageable);
        } else {
            result = reportService.listAll(hospitalId, pageable);
        }

        return ResponseEntity.ok(result);
    }

    // ─── GET /api/reports/{id} ────────────────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('Doctor', 'Nurse', 'admin', 'superadmin')")
    public ResponseEntity<MedicalReport> getById(
            @PathVariable Integer id,
            Authentication authentication) {
        return ResponseEntity.ok(
                reportService.getById(id, JwtContextHolder.getHospitalId()));
    }

    // ─── PUT /api/reports/{id} ────────────────────────────────────────────────

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('Doctor', 'admin', 'superadmin')")
    public ResponseEntity<MedicalReport> update(
            @PathVariable Integer id,
            @RequestBody MedicalReport report,
            Authentication authentication) {
        return ResponseEntity.ok(
                reportService.update(id, report, JwtContextHolder.getHospitalId()));
    }

    // ─── DELETE /api/reports/{id} ─────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<Void> delete(
            @PathVariable Integer id,
            Authentication authentication) {
        reportService.delete(id, JwtContextHolder.getHospitalId());
        return ResponseEntity.noContent().build();
    }
}
