package com.insa.hospital.controller;

import com.insa.hospital.entity.Accountant;
import com.insa.hospital.exception.ResourceNotFoundException;
import com.insa.hospital.repository.AccountantRepository;
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
 * Accountant REST Controller
 * Base URL: /api/accountants
 *
 * CRUD for the legacy `accountant` table.
 * Only admin/superadmin can manage accountant profiles.
 */
@RestController
@RequestMapping("/api/accountants")
public class AccountantController {

    private final AccountantRepository accountantRepository;

    @Autowired
    public AccountantController(AccountantRepository accountantRepository) {
        this.accountantRepository = accountantRepository;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<Accountant> create(
            @RequestBody Accountant accountant,
            Authentication authentication) {
        accountant.setHospitalId(JwtContextHolder.getHospitalId());
        accountant.setEmail(StaffEmailPolicy.normalizeStaffEmail(accountant.getEmail()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountantRepository.save(accountant));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<Page<Accountant>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String hospitalId = JwtContextHolder.getHospitalId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        return ResponseEntity.ok(accountantRepository.findByHospitalId(hospitalId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<Accountant> getById(
            @PathVariable Integer id,
            Authentication authentication) {
        String hospitalId = JwtContextHolder.getHospitalId();
        Accountant accountant = accountantRepository.findById(id)
                .filter(a -> hospitalId.equals(a.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Accountant", "id", id));
        return ResponseEntity.ok(accountant);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<Accountant> update(
            @PathVariable Integer id,
            @RequestBody Accountant updated,
            Authentication authentication) {
        String hospitalId = JwtContextHolder.getHospitalId();
        Accountant existing = accountantRepository.findById(id)
                .filter(a -> hospitalId.equals(a.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Accountant", "id", id));
        existing.setImgUrl(updated.getImgUrl());
        existing.setName(updated.getName());
        existing.setEmail(StaffEmailPolicy.normalizeStaffEmail(updated.getEmail()));
        existing.setAddress(updated.getAddress());
        existing.setPhone(updated.getPhone());
        existing.setX(updated.getX());
        existing.setIonUserId(updated.getIonUserId());
        return ResponseEntity.ok(accountantRepository.save(existing));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<Void> delete(
            @PathVariable Integer id,
            Authentication authentication) {
        String hospitalId = JwtContextHolder.getHospitalId();
        Accountant existing = accountantRepository.findById(id)
                .filter(a -> hospitalId.equals(a.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Accountant", "id", id));
        accountantRepository.delete(existing);
        return ResponseEntity.noContent().build();
    }
}
