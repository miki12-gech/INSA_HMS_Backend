package com.insa.hospital.controller;

import com.insa.hospital.dto.DiagnosisCategoryDto;
import com.insa.hospital.dto.DiagnosisCategoryRequestDto;
import com.insa.hospital.service.DiagnosisCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/diagnosis-categories")
@RequiredArgsConstructor
public class DiagnosisCategoryController {

    private final DiagnosisCategoryService diagnosisCategoryService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DiagnosisCategoryDto>> list(@RequestParam(required = false) String query) {
        return ResponseEntity.ok(diagnosisCategoryService.list(query));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DiagnosisCategoryDto> create(@Valid @RequestBody DiagnosisCategoryRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(diagnosisCategoryService.create(dto));
    }
}
