package com.insa.hospital.controller;

import com.insa.hospital.dto.ProfileRequestDto;
import com.insa.hospital.dto.ProfileResponseDto;
import com.insa.hospital.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileResponseDto> getProfile() {
        return ResponseEntity.ok(profileService.getProfile());
    }

    @RequestMapping(method = {RequestMethod.POST, RequestMethod.PUT})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileResponseDto> updateProfile(@Valid @RequestBody ProfileRequestDto dto) {
        return ResponseEntity.ok(profileService.updateProfile(dto));
    }
}
