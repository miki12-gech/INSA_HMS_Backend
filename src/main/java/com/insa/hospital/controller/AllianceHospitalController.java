package com.insa.hospital.controller;

import com.insa.hospital.dto.AllianceHospitalRequestDto;
import com.insa.hospital.dto.AllianceHospitalResponseDto;
import com.insa.hospital.security.JwtContextHolder;
import com.insa.hospital.service.AllianceHospitalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alliance-hospitals")
@RequiredArgsConstructor
public class AllianceHospitalController {

    private final AllianceHospitalService allianceHospitalService;

    @GetMapping("/list")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<List<AllianceHospitalResponseDto>> listHospitals() {
        return ResponseEntity.ok(
                allianceHospitalService.listHospitals(JwtContextHolder.getHospitalId()));
    }

    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AllianceHospitalResponseDto>> listActiveHospitals() {
        return ResponseEntity.ok(
                allianceHospitalService.listActiveHospitals(JwtContextHolder.getHospitalId()));
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<AllianceHospitalResponseDto> addHospital(
            @Valid @RequestBody AllianceHospitalRequestDto dto) {

        AllianceHospitalResponseDto created = allianceHospitalService.addHospital(
                dto, JwtContextHolder.getHospitalId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
