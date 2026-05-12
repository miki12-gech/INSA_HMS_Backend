package com.insa.hospital.controller;

import com.insa.hospital.dto.*;
import com.insa.hospital.service.LabService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lab")
@RequiredArgsConstructor
public class LabController {

    private final LabService labService;

    // --- Lab Tests ---
    @GetMapping
    public ResponseEntity<List<LabResponseDto>> getLabs() {
        return ResponseEntity.ok(labService.getAllLabs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LabResponseDto> getLab(@PathVariable Long id) {
        LabResponseDto lab = labService.getLabById(id);
        if (lab != null) {
            return ResponseEntity.ok(lab);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/add")
    public ResponseEntity<?> addLab(@RequestBody LabRequestDto request) {
        labService.saveLab(request);
        return ResponseEntity.ok().body("{\"message\":\"Lab added successfully!\"}");
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateLab(@PathVariable Long id, @RequestBody LabRequestDto request) {
        // Reconstruct the DTO with the path id so LabService.saveLab() updates the correct record
        LabRequestDto withId = new LabRequestDto(
                id,
                request.report(),
                request.patient(),
                request.date(),
                request.doctor(),
                request.status(),
                request.templet_id(),
                request.p_name(),
                request.p_email(),
                request.p_phone(),
                request.p_age(),
                request.p_gender(),
                request.d_name(),
                request.d_email(),
                request.d_phone(),
                request.discount(),
                request.amount_received()
        );
        labService.saveLab(withId);
        return ResponseEntity.ok().body("{\"message\":\"Lab updated successfully!\"}");
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteLab(@PathVariable Long id) {
        labService.deleteLab(id);
        return ResponseEntity.ok().body("{\"message\":\"Lab deleted successfully!\"}");
    }

    // --- Lab Category ---
    @GetMapping("/category")
    public ResponseEntity<List<LabCategoryDto>> getCategories() {
        return ResponseEntity.ok(labService.getCategories());
    }

    @PostMapping("/addCategory")
    public ResponseEntity<?> addCategory(@RequestBody LabCategoryDto dto) {
        labService.saveCategory(dto);
        return ResponseEntity.ok().body("{\"message\":\"Lab Category added successfully!\"}");
    }

    @PutMapping("/updateCategory/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody LabCategoryDto dto) {
        labService.saveCategory(dto);
        return ResponseEntity.ok().body("{\"message\":\"Lab Category updated successfully!\"}");
    }

    @DeleteMapping("/deleteCategory/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        labService.deleteCategory(id);
        return ResponseEntity.ok().body("{\"message\":\"Lab Category deleted successfully!\"}");
    }

    // --- Template ---
    @GetMapping("/template")
    public ResponseEntity<List<TemplateDto>> getTemplates() {
        return ResponseEntity.ok(labService.getTemplates());
    }

    @PostMapping("/addTemplate")
    public ResponseEntity<?> addTemplate(@RequestBody TemplateDto dto) {
        labService.saveTemplate(dto);
        return ResponseEntity.ok().body("{\"message\":\"Template added successfully!\"}");
    }

    @PutMapping("/updateTemplate/{id}")
    public ResponseEntity<?> updateTemplate(@PathVariable Long id, @RequestBody TemplateDto dto) {
        labService.saveTemplate(dto);
        return ResponseEntity.ok().body("{\"message\":\"Template updated successfully!\"}");
    }

    @DeleteMapping("/deleteTemplate/{id}")
    public ResponseEntity<?> deleteTemplate(@PathVariable Long id) {
        labService.deleteTemplate(id);
        return ResponseEntity.ok().body("{\"message\":\"Template deleted successfully!\"}");
    }
}
