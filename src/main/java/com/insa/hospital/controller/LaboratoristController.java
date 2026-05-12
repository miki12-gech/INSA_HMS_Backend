package com.insa.hospital.controller;

import com.insa.hospital.entity.Laboratorist;
import com.insa.hospital.exception.ResourceNotFoundException;
import com.insa.hospital.repository.LaboratoristRepository;
import com.insa.hospital.security.JwtContextHolder;
import com.insa.hospital.util.StaffEmailPolicy;
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

/**
 * Laboratorist REST Controller
 * Base URL: /api/laboratorists
 *
 * CRUD for the legacy `laboratorist` table.
 * Admin/superadmin manage profiles; Laboratorist role can view their own.
 */
@RestController
@RequestMapping("/api/laboratorists")
public class LaboratoristController {

    private final LaboratoristRepository laboratoristRepository;

    @Autowired
    public LaboratoristController(LaboratoristRepository laboratoristRepository) {
        this.laboratoristRepository = laboratoristRepository;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<Laboratorist> create(
            @RequestBody Laboratorist laboratorist,
            Authentication authentication) {
        laboratorist.setHospitalId(JwtContextHolder.getHospitalId());
        laboratorist.setEmail(StaffEmailPolicy.normalizeStaffEmail(laboratorist.getEmail()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(laboratoristRepository.save(laboratorist));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('Laboratorist', 'admin', 'superadmin')")
    public ResponseEntity<Page<Laboratorist>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String hospitalId = JwtContextHolder.getHospitalId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        return ResponseEntity.ok(laboratoristRepository.findByHospitalId(hospitalId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('Laboratorist', 'admin', 'superadmin')")
    public ResponseEntity<Laboratorist> getById(
            @PathVariable Integer id,
            Authentication authentication) {
        String hospitalId = JwtContextHolder.getHospitalId();
        Laboratorist lab = laboratoristRepository.findById(id)
                .filter(l -> hospitalId.equals(l.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Laboratorist", "id", id));
        return ResponseEntity.ok(lab);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<Laboratorist> update(
            @PathVariable Integer id,
            @RequestBody Laboratorist updated,
            Authentication authentication) {
        String hospitalId = JwtContextHolder.getHospitalId();
        Laboratorist existing = laboratoristRepository.findById(id)
                .filter(l -> hospitalId.equals(l.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Laboratorist", "id", id));
        existing.setImgUrl(updated.getImgUrl());
        existing.setName(updated.getName());
        existing.setEmail(StaffEmailPolicy.normalizeStaffEmail(updated.getEmail()));
        existing.setAddress(updated.getAddress());
        existing.setPhone(updated.getPhone());
        existing.setX(updated.getX());
        existing.setY(updated.getY());
        existing.setIonUserId(updated.getIonUserId());
        return ResponseEntity.ok(laboratoristRepository.save(existing));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<Void> delete(
            @PathVariable Integer id,
            Authentication authentication) {
        String hospitalId = JwtContextHolder.getHospitalId();
        Laboratorist existing = laboratoristRepository.findById(id)
                .filter(l -> hospitalId.equals(l.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Laboratorist", "id", id));
        laboratoristRepository.delete(existing);
        return ResponseEntity.noContent().build();
    }
}
