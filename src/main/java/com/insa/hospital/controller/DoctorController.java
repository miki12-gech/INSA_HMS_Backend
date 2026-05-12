package com.insa.hospital.controller;

import com.insa.hospital.dto.DoctorDhsReportResponseDto;
import com.insa.hospital.dto.DoctorRequestDto;
import com.insa.hospital.dto.DoctorResponseDto;
import com.insa.hospital.dto.MedicalHistoryDto;
import com.insa.hospital.dto.PatientMedicalHistoryResponseDto;
import com.insa.hospital.dto.PatientNoteDto;
import com.insa.hospital.dto.PrescriptionRequestDto;
import com.insa.hospital.dto.PrescriptionResponseDto;
import com.insa.hospital.security.JwtContextHolder;
import com.insa.hospital.service.DoctorService;
import com.insa.hospital.service.PrescriptionService;
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

import java.time.LocalDate;
import java.util.List;

/**
 * Doctor REST Controller
 * Base URL: /api/doctors
 *
 * RBAC (from legacy doctor.php __construct line 17):
 *   controller-level: admin, Accountant, Doctor
 *   write ops (create/update/delete): admin, Accountant only
 *   read ops: any authenticated user
 *
 * AUDIT FIX: Changed hasAnyRole() → hasAnyAuthority().
 * Spring Security's hasAnyRole() auto-prepends "ROLE_", which
 * does NOT match our authority strings ('admin', 'Doctor', etc.)
 * stored without that prefix from Ion Auth groups.
 */
@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorService doctorService;
    private final PrescriptionService prescriptionService;

    @Autowired
    public DoctorController(DoctorService doctorService, PrescriptionService prescriptionService) {
        this.doctorService = doctorService;
        this.prescriptionService = prescriptionService;
    }

    // ─── POST /api/doctors ────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin', 'Accountant')")
    public ResponseEntity<DoctorResponseDto> createDoctor(
            @Valid @RequestBody DoctorRequestDto dto,
            Authentication authentication) {

        String hospitalId = JwtContextHolder.getHospitalId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(doctorService.createDoctor(dto, hospitalId));
    }

    // ─── GET /api/doctors/all ─────────────────────────────────────────────────

    /**
     * Unpaged list of all doctors for dropdown population.
     * Mirrors /api/patients/all — used by lab forms, appointment forms, etc.
     */
    @GetMapping("/all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DoctorResponseDto>> getAllDoctors(Authentication authentication) {
        String hospitalId = JwtContextHolder.getHospitalId();
        return ResponseEntity.ok(doctorService.listAllDoctors(hospitalId));
    }

    // ─── GET /api/doctors ─────────────────────────────────────────────────────

    /**
     * Paginated list of doctors with optional search.
     * Also supports ?all=true for unpaged dropdown list.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> listDoctors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "false") boolean all,
            Authentication authentication) {

        String hospitalId = JwtContextHolder.getHospitalId();

        // ?all=true → unpaged list for dropdowns (e.g. appointment forms)
        if (all) {
            List<DoctorResponseDto> doctors = doctorService.listAllDoctors(hospitalId);
            return ResponseEntity.ok(doctors);
        }

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<DoctorResponseDto> result = StringUtils.hasText(search)
                ? doctorService.searchDoctors(hospitalId, search, pageable)
                : doctorService.listDoctors(hospitalId, pageable);

        return ResponseEntity.ok(result);
    }

    // ─── GET /api/doctors/{id} ────────────────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DoctorResponseDto> getDoctor(
            @PathVariable Integer id,
            Authentication authentication) {

        String hospitalId = JwtContextHolder.getHospitalId();
        return ResponseEntity.ok(doctorService.getDoctorById(id, hospitalId));
    }

    // ─── PUT /api/doctors/{id} ────────────────────────────────────────────────

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin', 'Accountant')")
    public ResponseEntity<DoctorResponseDto> updateDoctor(
            @PathVariable Integer id,
            @Valid @RequestBody DoctorRequestDto dto,
            Authentication authentication) {

        String hospitalId = JwtContextHolder.getHospitalId();
        return ResponseEntity.ok(doctorService.updateDoctor(id, dto, hospitalId));
    }

    // ─── DELETE /api/doctors/{id} ─────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin', 'Accountant')")
    public ResponseEntity<Void> deleteDoctor(
            @PathVariable Integer id,
            Authentication authentication) {

        String hospitalId = JwtContextHolder.getHospitalId();
        doctorService.deleteDoctor(id, hospitalId);
        return ResponseEntity.noContent().build();
    }

    // ─── Phase 6: Consultations, Notes, Prescriptions ─────────────────────────

    @PostMapping("/consultation")
    @PreAuthorize("hasAuthority('Doctor')")
    public ResponseEntity<MedicalHistoryDto> saveConsultation(
            @RequestBody MedicalHistoryDto dto,
            Authentication authentication) {
        
        String hospitalId = JwtContextHolder.getHospitalId();
        dto.setHospitalId(hospitalId);
        // Fallback for Doctor Identity, typically Auth principal or name based on your token setup
        String doctorId = authentication.getName(); 
        return ResponseEntity.ok(doctorService.saveMedicalHistory(dto, doctorId));
    }

    @PostMapping("/note")
    @PreAuthorize("hasAuthority('Doctor')")
    public ResponseEntity<PatientNoteDto> savePatientNote(
            @RequestBody PatientNoteDto dto,
            Authentication authentication) {
        
        String hospitalId = JwtContextHolder.getHospitalId();
        dto.setHospitalId(hospitalId);
        String doctorId = authentication.getName();
        return ResponseEntity.ok(doctorService.savePatientNote(dto, doctorId));
    }

    @PostMapping("/prescription")
    @PreAuthorize("hasAuthority('Doctor')")
    public ResponseEntity<PrescriptionResponseDto> savePrescription(
            @RequestBody PrescriptionRequestDto dto,
            Authentication authentication) {
        
        String hospitalId = JwtContextHolder.getHospitalId();
        String doctorId = authentication.getName();
        return ResponseEntity.ok(prescriptionService.createPrescription(dto, hospitalId, doctorId));
    }

    @GetMapping("/history/{patientId}")
    @PreAuthorize("hasAuthority('Doctor')")
    public ResponseEntity<PatientMedicalHistoryResponseDto> getHistory(
            @PathVariable String patientId,
            Authentication authentication) {
        
        String hospitalId = JwtContextHolder.getHospitalId();
        return ResponseEntity.ok(doctorService.getPatientFullHistory(patientId, hospitalId));
    }

    @GetMapping("/dhs-report")
    @PreAuthorize("hasAuthority('Doctor')")
    public ResponseEntity<DoctorDhsReportResponseDto> getDhsReport(
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @RequestParam(required = false) String department) {

        String hospitalId = JwtContextHolder.getHospitalId();
        return ResponseEntity.ok(doctorService.getDhsReport(hospitalId, dateFrom, dateTo, department));
    }
}
