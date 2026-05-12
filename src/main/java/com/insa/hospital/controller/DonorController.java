package com.insa.hospital.controller;

import com.insa.hospital.entity.Donor;
import com.insa.hospital.security.JwtContextHolder;
import com.insa.hospital.service.DonorService;
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
 * Donor REST Controller
 * Base URL: /api/donors
 *
 * Manages blood donors in the legacy `donor` table.
 *
 * Roles:
 *  - View: Nurse, Doctor, admin, superadmin
 *  - Create/Update/Delete: admin, superadmin
 */
@RestController
@RequestMapping("/api/donors")
public class DonorController {

    private final DonorService donorService;

    @Autowired
    public DonorController(DonorService donorService) {
        this.donorService = donorService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<Donor> create(
            @RequestBody Donor donor,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(donorService.createDonor(donor, JwtContextHolder.getHospitalId()));
    }

    /**
     * List donors with optional blood group filter.
     * ?group=A%2B or ?group=B-
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('Nurse', 'Doctor', 'admin', 'superadmin')")
    public ResponseEntity<Page<Donor>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String group,
            Authentication authentication) {

        String hospitalId = JwtContextHolder.getHospitalId();
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Donor> result = StringUtils.hasText(group)
                ? donorService.listByGroup(group, hospitalId, pageable)
                : donorService.listAll(hospitalId, pageable);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('Nurse', 'Doctor', 'admin', 'superadmin')")
    public ResponseEntity<Donor> getById(
            @PathVariable Integer id,
            Authentication authentication) {
        return ResponseEntity.ok(
                donorService.getById(id, JwtContextHolder.getHospitalId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<Donor> update(
            @PathVariable Integer id,
            @RequestBody Donor donor,
            Authentication authentication) {
        return ResponseEntity.ok(
                donorService.update(id, donor, JwtContextHolder.getHospitalId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<Void> delete(
            @PathVariable Integer id,
            Authentication authentication) {
        donorService.delete(id, JwtContextHolder.getHospitalId());
        return ResponseEntity.noContent().build();
    }
}
