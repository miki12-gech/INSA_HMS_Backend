package com.insa.hospital.controller;

import com.insa.hospital.entity.AllotedBed;
import com.insa.hospital.entity.Bed;
import com.insa.hospital.entity.BedCategory;
import com.insa.hospital.service.BedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bed")
@RequiredArgsConstructor
public class BedController {

    private final BedService bedService;

    // ─── Bed Endpoints ────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Bed>> getBeds() {
        return ResponseEntity.ok(bedService.getBeds());
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Bed> getBedById(@PathVariable Long id) {
        Bed bed = bedService.getBedById(id);
        return bed != null ? ResponseEntity.ok(bed) : ResponseEntity.notFound().build();
    }

    @PostMapping("/addBed")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin', 'Doctor', 'Nurse', 'Receptionist')")
    public ResponseEntity<Bed> addBed(@RequestBody Bed bed) {
        bedService.saveBed(bed);
        return ResponseEntity.status(HttpStatus.CREATED).body(bed);
    }
    
    @PutMapping("/updateBed/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin', 'Doctor', 'Nurse', 'Receptionist')")
    public ResponseEntity<Bed> updateBed(@PathVariable Long id, @RequestBody Bed bed) {
        bedService.updateBed(id, bed);
        return ResponseEntity.ok(bed);
    }

    @DeleteMapping("/deleteBed/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<Void> deleteBed(@PathVariable Long id) {
        bedService.deleteBed(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Bed Category Endpoints ───────────────────────────────────────────────

    @GetMapping("/bedCategory")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BedCategory>> getBedCategories() {
        return ResponseEntity.ok(bedService.getBedCategories());
    }

    @GetMapping("/bedCategory/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BedCategory> getBedCategoryById(@PathVariable Long id) {
        BedCategory category = bedService.getBedCategoryById(id);
        return category != null ? ResponseEntity.ok(category) : ResponseEntity.notFound().build();
    }

    @PostMapping("/addCategory")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin', 'Doctor', 'Nurse', 'Receptionist')")
    public ResponseEntity<BedCategory> addBedCategory(@RequestBody BedCategory category) {
        bedService.saveBedCategory(category);
        return ResponseEntity.status(HttpStatus.CREATED).body(category);
    }
    
    @PutMapping("/updateCategory/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin', 'Doctor', 'Nurse', 'Receptionist')")
    public ResponseEntity<BedCategory> updateBedCategory(@PathVariable Long id, @RequestBody BedCategory category) {
        bedService.updateBedCategory(id, category);
        return ResponseEntity.ok(category);
    }

    @DeleteMapping("/deleteBedCategory/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<Void> deleteBedCategory(@PathVariable Long id) {
        bedService.deleteBedCategory(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Alloted Bed Endpoints ────────────────────────────────────────────────

    @GetMapping("/bedAllotment")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AllotedBed>> getAllotedBeds() {
        return ResponseEntity.ok(bedService.getAllotedBeds());
    }

    @GetMapping("/bedAllotment/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AllotedBed> getAllotedBedById(@PathVariable Long id) {
        AllotedBed allotedBed = bedService.getAllotedBedById(id);
        return allotedBed != null ? ResponseEntity.ok(allotedBed) : ResponseEntity.notFound().build();
    }

    @PostMapping("/addAllotment")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin', 'Doctor', 'Nurse', 'Receptionist')")
    public ResponseEntity<AllotedBed> addAllotment(@RequestBody AllotedBed allotedBed) {
        bedService.saveAllotedBed(allotedBed);
        return ResponseEntity.status(HttpStatus.CREATED).body(allotedBed);
    }
    
    @PutMapping("/updateAllotment/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin', 'Doctor', 'Nurse', 'Receptionist')")
    public ResponseEntity<AllotedBed> updateAllotment(@PathVariable Long id, @RequestBody AllotedBed allotedBed) {
        bedService.updateAllotedBed(id, allotedBed);
        return ResponseEntity.ok(allotedBed);
    }

    @DeleteMapping("/deleteAllotment/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin', 'Doctor', 'Nurse', 'Receptionist')")
    public ResponseEntity<Void> deleteAllotment(@PathVariable Long id) {
        bedService.deleteAllotedBed(id);
        return ResponseEntity.noContent().build();
    }
}
