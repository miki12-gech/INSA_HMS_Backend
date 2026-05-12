package com.insa.hospital.controller;

import com.insa.hospital.entity.Settings;
import com.insa.hospital.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {
    private final SettingsService settingsService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Settings> getSettings(@RequestParam String hospitalId) {
        return ResponseEntity.ok(settingsService.getSettings(hospitalId));
    }

    @GetMapping("/public")
    public ResponseEntity<Settings> getPublicSettings(@RequestParam String hospitalId) {
        return ResponseEntity.ok(settingsService.getSettings(hospitalId));
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('ROLE_superadmin', 'ROLE_admin')")
    public ResponseEntity<Settings> updateSettings(@RequestParam String hospitalId, @RequestBody Settings settings) {
        settingsService.validateHospitalManagementAccess(hospitalId);
        return ResponseEntity.ok(settingsService.updateSettings(hospitalId, settings));
    }

    @PostMapping("/upload-logo")
    @PreAuthorize("hasAnyAuthority('ROLE_superadmin', 'ROLE_admin')")
    public ResponseEntity<String> uploadLogo(@RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(settingsService.uploadLogo(file));
    }

    @GetMapping("/language")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getLanguage(@RequestParam String hospitalId) {
        Settings settings = settingsService.getSettings(hospitalId);
        if (settings != null) {
            return ResponseEntity.ok(settings);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/changeLanguage")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changeLanguage(@RequestParam String hospitalId, @RequestParam String language) {
        Settings settings = settingsService.getSettings(hospitalId);
        if (settings == null) {
            settings = new Settings();
            settings.setHospitalId(hospitalId);
        }
        settings.setLanguage(language);
        Settings updatedSettings = settingsService.updateSettings(hospitalId, settings);
        return ResponseEntity.ok(updatedSettings);
    }
}

