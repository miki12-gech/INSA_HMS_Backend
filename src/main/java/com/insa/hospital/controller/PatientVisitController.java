package com.insa.hospital.controller;

import com.insa.hospital.dto.PatientVisitResponseDto;
import com.insa.hospital.dto.PatientVisitStartRequestDto;
import com.insa.hospital.dto.PatientVisitStatusUpdateRequestDto;
import com.insa.hospital.security.JwtContextHolder;
import com.insa.hospital.service.PatientVisitService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/visits")
public class PatientVisitController {

    private final PatientVisitService patientVisitService;

    public PatientVisitController(PatientVisitService patientVisitService) {
        this.patientVisitService = patientVisitService;
    }

    @PostMapping("/start")
    public ResponseEntity<PatientVisitResponseDto> startVisit(
            @Valid @RequestBody PatientVisitStartRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(patientVisitService.startVisit(dto, JwtContextHolder.getHospitalId()));
    }

    @PostMapping
    public ResponseEntity<PatientVisitResponseDto> createVisit(
            @Valid @RequestBody PatientVisitStartRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(patientVisitService.startVisit(dto, JwtContextHolder.getHospitalId()));
    }

    @GetMapping
    public ResponseEntity<List<PatientVisitResponseDto>> listVisits(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String visitType) {
        return ResponseEntity.ok(patientVisitService.listVisits(status, visitType));
    }

    @GetMapping("/pending-triage")
    public ResponseEntity<List<PatientVisitResponseDto>> pendingTriage() {
        return ResponseEntity.ok(patientVisitService.listVisits("WAITING_TRIAGE", null));
    }

    @GetMapping("/emergency")
    public ResponseEntity<List<PatientVisitResponseDto>> emergencyVisits(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(patientVisitService.listVisits(status, "EMERGENCY"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PatientVisitResponseDto> getVisit(@PathVariable Long id) {
        return ResponseEntity.ok(patientVisitService.getVisit(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PatientVisitResponseDto> updateVisitStatus(
            @PathVariable Long id,
            @Valid @RequestBody PatientVisitStatusUpdateRequestDto dto) {
        return ResponseEntity.ok(patientVisitService.updateVisitStatus(id, dto, JwtContextHolder.getHospitalId()));
    }

    @PostMapping("/status")
    public ResponseEntity<PatientVisitResponseDto> updateVisitStatus(
            @Valid @RequestBody PatientVisitStatusUpdateRequestDto dto) {
        return ResponseEntity.ok(patientVisitService.updateVisitStatus(dto, JwtContextHolder.getHospitalId()));
    }
}
