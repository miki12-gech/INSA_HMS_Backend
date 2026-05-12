package com.insa.hospital.controller;

import com.insa.hospital.dto.MedicalHistoryRequestDto;
import com.insa.hospital.dto.MedicalHistoryResponseDto;
import com.insa.hospital.dto.PatientNoteRequestDto;
import com.insa.hospital.dto.PatientNoteResponseDto;
import com.insa.hospital.security.JwtContextHolder;
import com.insa.hospital.service.TreatmentHistoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Treatment History REST Controller
 * Base URL: /api/treatment-history
 *
 * Exposes endpoints for 'Medical History' (Case History) and 'Patient Notes'.
 * Replicates the legacy patient.php medicalHistory() module logic perfectly.
 */
@RestController
@RequestMapping("/api/treatment-history")
public class TreatmentHistoryController {

    private final TreatmentHistoryService treatmentHistoryService;

    @Autowired
    public TreatmentHistoryController(TreatmentHistoryService treatmentHistoryService) {
        this.treatmentHistoryService = treatmentHistoryService;
    }

    // ─── MEDICAL HISTORY (CASE HISTORY) ──────────────────────────────────────

    @GetMapping("/medical-history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MedicalHistoryResponseDto>> getMedicalHistories(
            @RequestParam String patientId) {
        String hospitalId = JwtContextHolder.getHospitalId();
        return ResponseEntity.ok(treatmentHistoryService.getMedicalHistoriesByPatientId(patientId, hospitalId));
    }

    @PostMapping("/medical-history")
    @PreAuthorize("hasAnyAuthority('Admin', 'Superadmin', 'Doctor', 'Receptionist', 'Nurse')")
    public ResponseEntity<MedicalHistoryResponseDto> addMedicalHistory(
            @Valid @RequestBody MedicalHistoryRequestDto dto) {
        String hospitalId = JwtContextHolder.getHospitalId();
        MedicalHistoryResponseDto responseDto = treatmentHistoryService.addMedicalHistory(dto, hospitalId);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @PutMapping("/medical-history/{id}")
    @PreAuthorize("hasAnyAuthority('Admin', 'Superadmin', 'Doctor', 'Receptionist', 'Nurse')")
    public ResponseEntity<MedicalHistoryResponseDto> updateMedicalHistory(
            @PathVariable Integer id,
            @Valid @RequestBody MedicalHistoryRequestDto dto) {
        String hospitalId = JwtContextHolder.getHospitalId();
        return ResponseEntity.ok(treatmentHistoryService.updateMedicalHistory(id, dto, hospitalId));
    }

    @DeleteMapping("/medical-history/{id}")
    @PreAuthorize("hasAnyAuthority('Admin', 'Superadmin', 'Doctor')")
    public ResponseEntity<Void> deleteMedicalHistory(@PathVariable Integer id) {
        String hospitalId = JwtContextHolder.getHospitalId();
        treatmentHistoryService.deleteMedicalHistory(id, hospitalId);
        return ResponseEntity.noContent().build();
    }

    // ─── PATIENT NOTES ───────────────────────────────────────────────────────

    @GetMapping("/notes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PatientNoteResponseDto>> getPatientNotes(
            @RequestParam String patientId) {
        String hospitalId = JwtContextHolder.getHospitalId();
        return ResponseEntity.ok(treatmentHistoryService.getPatientNotesByPatientId(patientId, hospitalId));
    }

    @PostMapping("/notes")
    @PreAuthorize("hasAnyAuthority('Admin', 'Superadmin', 'Doctor', 'Receptionist', 'Nurse')")
    public ResponseEntity<PatientNoteResponseDto> addPatientNote(
            @Valid @RequestBody PatientNoteRequestDto dto) {
        String hospitalId = JwtContextHolder.getHospitalId();
        String doctorIonUserId = JwtContextHolder.getUserId();
        PatientNoteResponseDto responseDto = treatmentHistoryService.addPatientNote(dto, hospitalId, doctorIonUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @PutMapping("/notes/{id}")
    @PreAuthorize("hasAnyAuthority('Admin', 'Superadmin', 'Doctor', 'Receptionist', 'Nurse')")
    public ResponseEntity<PatientNoteResponseDto> updatePatientNote(
            @PathVariable Integer id,
            @Valid @RequestBody PatientNoteRequestDto dto) {
        String hospitalId = JwtContextHolder.getHospitalId();
        return ResponseEntity.ok(treatmentHistoryService.updatePatientNote(id, dto, hospitalId));
    }

    @DeleteMapping("/notes/{id}")
    @PreAuthorize("hasAnyAuthority('Admin', 'Superadmin', 'Doctor')")
    public ResponseEntity<Void> deletePatientNote(@PathVariable Integer id) {
        String hospitalId = JwtContextHolder.getHospitalId();
        treatmentHistoryService.deletePatientNote(id, hospitalId);
        return ResponseEntity.noContent().build();
    }
}
