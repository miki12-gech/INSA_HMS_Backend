package com.insa.hospital.controller;

import com.insa.hospital.dto.AdminUserCreateRequestDto;
import com.insa.hospital.dto.AdminUserDefaultPasswordDto;
import com.insa.hospital.dto.AdminUserPasswordResetDto;
import com.insa.hospital.dto.AdminUserResponseDto;
import com.insa.hospital.dto.AdminUserStatusUpdateDto;
import com.insa.hospital.security.JwtContextHolder;
import com.insa.hospital.service.UserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Superadmin IAM endpoints for staff and credentials.
 *
 * Base URL: /api/admin/users
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserManagementService userManagementService;

    @GetMapping("/list")
    public ResponseEntity<List<AdminUserResponseDto>> listUsers(
            @RequestParam(required = false) String role) {
        return ResponseEntity.ok(
                userManagementService.listUsers(JwtContextHolder.getHospitalId(), role)
        );
    }

    @PostMapping("/add")
    public ResponseEntity<AdminUserResponseDto> addUser(
            @Valid @RequestBody AdminUserCreateRequestDto dto) {
        AdminUserResponseDto created = userManagementService.createUser(
                JwtContextHolder.getHospitalId(), dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/default-password")
    public ResponseEntity<AdminUserDefaultPasswordDto> getDefaultPassword() {
        return ResponseEntity.ok(userManagementService.getDefaultPassword());
    }

    @PutMapping("/default-password")
    public ResponseEntity<AdminUserDefaultPasswordDto> updateDefaultPassword(
            @Valid @RequestBody AdminUserDefaultPasswordDto dto) {
        return ResponseEntity.ok(userManagementService.updateDefaultPassword(dto));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<AdminUserResponseDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminUserStatusUpdateDto dto) {
        return ResponseEntity.ok(
                userManagementService.updateStatus(JwtContextHolder.getHospitalId(), id, dto)
        );
    }

    @PutMapping("/{id}/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody AdminUserPasswordResetDto dto) {
        userManagementService.resetPassword(JwtContextHolder.getHospitalId(), id, dto);
        return ResponseEntity.ok(Map.of(
                "message", "Password reset successfully.",
                "temporaryPassword", dto.newPassword() != null && !dto.newPassword().isBlank()
                        ? dto.newPassword().trim()
                        : userManagementService.getDefaultPassword().password()
        ));
    }
}
