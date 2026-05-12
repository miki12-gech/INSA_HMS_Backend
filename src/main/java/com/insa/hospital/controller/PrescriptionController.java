package com.insa.hospital.controller;

import com.insa.hospital.dto.PrescriptionRequestDto;
import com.insa.hospital.dto.PrescriptionResponseDto;
import com.insa.hospital.entity.Prescription;
import com.insa.hospital.repository.DoctorRepository;
import com.insa.hospital.repository.PatientRepository;
import com.insa.hospital.repository.PrescriptionRepository;
import com.insa.hospital.security.JwtContextHolder;
import com.insa.hospital.service.PrescriptionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Prescription REST Controller
 * Base URL: /api/prescriptions
 *
 * Resolves patient/doctor names from their respective tables
 * so the frontend gets human-readable data in every response.
 */
@RestController
@RequestMapping("/api/prescriptions")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final PrescriptionRepository prescriptionRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    @Autowired
    public PrescriptionController(PrescriptionService prescriptionService,
                                  PrescriptionRepository prescriptionRepository,
                                  PatientRepository patientRepository,
                                  DoctorRepository doctorRepository) {
        this.prescriptionService = prescriptionService;
        this.prescriptionRepository = prescriptionRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    // ─── GET /api/prescriptions ──────────────────────────────────────────────
    // List ALL prescriptions for the hospital (newest first)

    @GetMapping
    @PreAuthorize("hasAnyAuthority('Doctor', 'admin', 'superadmin', 'Pharmacist')")
    public ResponseEntity<List<PrescriptionResponseDto>> listAll() {
        String hospitalId = JwtContextHolder.getHospitalId();
        List<Prescription> prescriptions = prescriptionService.listAllVisibleEntities();
        List<PrescriptionResponseDto> result = resolveNames(prescriptions, hospitalId);
        return ResponseEntity.ok(result);
    }

    // ─── GET /api/prescriptions/pending ──────────────────────────────────────
    // List only prescriptions NOT yet dispensed/completed

    @GetMapping("/pending")
    @PreAuthorize("hasAnyAuthority('Doctor', 'admin', 'superadmin', 'Pharmacist')")
    public ResponseEntity<List<PrescriptionResponseDto>> listPending() {
        String hospitalId = JwtContextHolder.getHospitalId();
        List<String> excludedStates = Arrays.asList("DISPENSED", "COMPLETED", "SOLD");
        List<Prescription> prescriptions = prescriptionService
                .listPendingVisibleEntities(excludedStates);
        List<PrescriptionResponseDto> result = resolveNames(prescriptions, hospitalId);
        return ResponseEntity.ok(result);
    }

    // ─── POST /api/prescriptions/add ─────────────────────────────────────────

    @PostMapping("/add")
    @PreAuthorize("hasAnyAuthority('Doctor', 'admin', 'superadmin')")
    public ResponseEntity<PrescriptionResponseDto> addPrescription(
            @Valid @RequestBody PrescriptionRequestDto dto,
            Authentication authentication) {

        String hospitalId = JwtContextHolder.getHospitalId();
        String doctorId = JwtContextHolder.getUserId();

        PrescriptionResponseDto created = prescriptionService.createPrescription(dto, hospitalId, doctorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ─── GET /api/prescriptions/patient/{patientId} ──────────────────────────

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyAuthority('Doctor', 'admin', 'superadmin', 'Pharmacist')")
    public ResponseEntity<List<PrescriptionResponseDto>> getPatientPrescriptions(
            @PathVariable String patientId,
            Authentication authentication) {

        String hospitalId = JwtContextHolder.getHospitalId();
        List<Prescription> prescriptions = prescriptionService.listByPatientVisibleEntities(patientId);
        List<PrescriptionResponseDto> result = resolveNames(prescriptions, hospitalId);
        return ResponseEntity.ok(result);
    }

    // ─── Private: Batch name resolution ──────────────────────────────────────

    /**
     * Resolves patient and doctor names in bulk to avoid N+1 queries.
     * Collects all unique patient/doctor IDs, fetches them once,
     * then maps names onto each PrescriptionResponseDto.
     */
    private List<PrescriptionResponseDto> resolveNames(List<Prescription> prescriptions, String hospitalId) {
        // Collect all unique patient IDs and doctor IDs
        Set<Integer> patientIds = new HashSet<>();
        Set<String> doctorUserIds = new HashSet<>();

        for (Prescription p : prescriptions) {
            tryParseId(p.getPatient()).ifPresent(patientIds::add);
            if (p.getDoctor() != null && !p.getDoctor().isBlank() && !"0".equals(p.getDoctor().trim())) {
                doctorUserIds.add(p.getDoctor().trim());
            }
        }

        // Batch fetch all patients and doctors
        Map<Integer, String> patientNames = new HashMap<>();
        if (!patientIds.isEmpty()) {
            patientRepository.findAllById(patientIds)
                .forEach(pt -> patientNames.put(pt.getId(), pt.getName()));
        }

        Map<String, String> doctorNames = new HashMap<>();
        if (!doctorUserIds.isEmpty()) {
            doctorRepository.findByIonUserIdInAndHospitalId(new ArrayList<>(doctorUserIds), hospitalId)
                .forEach(doc -> doctorNames.put(doc.getIonUserId(), doc.getName()));
            if (doctorNames.size() < doctorUserIds.size()) {
                doctorRepository.findByIonUserIdIn(new ArrayList<>(doctorUserIds))
                    .forEach(doc -> doctorNames.putIfAbsent(doc.getIonUserId(), doc.getName()));
            }
        }

        // Map prescriptions to DTOs with resolved names
        return prescriptions.stream()
            .map(p -> {
                String pName = tryParseId(p.getPatient())
                    .map(patientNames::get)
                    .orElse(p.getPatient());
                String dName = doctorNames.getOrDefault(
                    p.getDoctor() != null ? p.getDoctor().trim() : "",
                    p.getDoctor()
                );
                return PrescriptionResponseDto.from(p, pName, dName);
            })
            .collect(Collectors.toList());
    }

    private Optional<Integer> tryParseId(String idStr) {
        if (idStr == null || idStr.isBlank() || "0".equals(idStr)) return Optional.empty();
        try {
            return Optional.of(Integer.parseInt(idStr.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
