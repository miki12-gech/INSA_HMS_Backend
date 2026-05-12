package com.insa.hospital.controller;

import com.insa.hospital.entity.Nurse;
import com.insa.hospital.exception.ResourceNotFoundException;
import com.insa.hospital.repository.NurseRepository;
import com.insa.hospital.security.JwtContextHolder;
import com.insa.hospital.util.StaffEmailPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller — Nurse Management endpoints.
 *
 * Base URL: /api/nurses
 * Provides CRUD for the legacy `nurse` table.
 */
@RestController
@RequestMapping("/api/nurses")
public class NurseController {

    private final NurseRepository nurseRepository;

    @Autowired
    public NurseController(NurseRepository nurseRepository) {
        this.nurseRepository = nurseRepository;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Nurse>> listAll() {
        return ResponseEntity.ok(nurseRepository.findByHospitalId(JwtContextHolder.getHospitalId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Nurse> getById(@PathVariable Integer id) {
        String hospitalId = JwtContextHolder.getHospitalId();
        Nurse nurse = nurseRepository.findById(id)
                .filter(n -> hospitalId.equals(n.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Nurse", "id", id));
        return ResponseEntity.ok(nurse);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<Nurse> create(@RequestBody Nurse nurse) {
        nurse.setHospitalId(JwtContextHolder.getHospitalId());
        nurse.setEmail(StaffEmailPolicy.normalizeStaffEmail(nurse.getEmail()));
        return ResponseEntity.ok(nurseRepository.save(nurse));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<Nurse> update(@PathVariable Integer id, @RequestBody Nurse body) {
        String hospitalId = JwtContextHolder.getHospitalId();
        Nurse nurse = nurseRepository.findById(id)
                .filter(n -> hospitalId.equals(n.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Nurse", "id", id));
        nurse.setName(body.getName());
        nurse.setEmail(StaffEmailPolicy.normalizeStaffEmail(body.getEmail()));
        nurse.setAddress(body.getAddress());
        nurse.setPhone(body.getPhone());
        nurse.setImgUrl(body.getImgUrl());
        nurse.setIonUserId(body.getIonUserId());
        return ResponseEntity.ok(nurseRepository.save(nurse));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        String hospitalId = JwtContextHolder.getHospitalId();
        Nurse nurse = nurseRepository.findById(id)
                .filter(n -> hospitalId.equals(n.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Nurse", "id", id));
        nurseRepository.delete(nurse);
        return ResponseEntity.noContent().build();
    }
}
