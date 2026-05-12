package com.insa.hospital.controller;

import com.insa.hospital.entity.Pharmacist;
import com.insa.hospital.exception.ResourceNotFoundException;
import com.insa.hospital.repository.PharmacistRepository;
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
 * Pharmacist REST Controller
 * Base URL: /api/pharmacists
 *
 * CRUD for the legacy `pharmacist` table.
 * Admin/superadmin manage profiles; Pharmacist role can view profiles.
 */
@RestController
@RequestMapping("/api/pharmacists")
public class PharmacistController {

    private final PharmacistRepository pharmacistRepository;

    @Autowired
    public PharmacistController(PharmacistRepository pharmacistRepository) {
        this.pharmacistRepository = pharmacistRepository;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<Pharmacist> create(
            @RequestBody Pharmacist pharmacist,
            Authentication authentication) {
        pharmacist.setHospitalId(JwtContextHolder.getHospitalId());
        pharmacist.setEmail(StaffEmailPolicy.normalizeStaffEmail(pharmacist.getEmail()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pharmacistRepository.save(pharmacist));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('Pharmacist', 'admin', 'superadmin')")
    public ResponseEntity<Page<Pharmacist>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String hospitalId = JwtContextHolder.getHospitalId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        return ResponseEntity.ok(pharmacistRepository.findByHospitalId(hospitalId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('Pharmacist', 'admin', 'superadmin')")
    public ResponseEntity<Pharmacist> getById(
            @PathVariable Integer id,
            Authentication authentication) {
        String hospitalId = JwtContextHolder.getHospitalId();
        Pharmacist pharmacist = pharmacistRepository.findById(id)
                .filter(p -> hospitalId.equals(p.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacist", "id", id));
        return ResponseEntity.ok(pharmacist);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<Pharmacist> update(
            @PathVariable Integer id,
            @RequestBody Pharmacist updated,
            Authentication authentication) {
        String hospitalId = JwtContextHolder.getHospitalId();
        Pharmacist existing = pharmacistRepository.findById(id)
                .filter(p -> hospitalId.equals(p.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacist", "id", id));
        existing.setImgUrl(updated.getImgUrl());
        existing.setName(updated.getName());
        existing.setEmail(StaffEmailPolicy.normalizeStaffEmail(updated.getEmail()));
        existing.setAddress(updated.getAddress());
        existing.setPhone(updated.getPhone());
        existing.setX(updated.getX());
        existing.setY(updated.getY());
        existing.setIonUserId(updated.getIonUserId());
        return ResponseEntity.ok(pharmacistRepository.save(existing));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<Void> delete(
            @PathVariable Integer id,
            Authentication authentication) {
        String hospitalId = JwtContextHolder.getHospitalId();
        Pharmacist existing = pharmacistRepository.findById(id)
                .filter(p -> hospitalId.equals(p.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacist", "id", id));
        pharmacistRepository.delete(existing);
        return ResponseEntity.noContent().build();
    }
}
