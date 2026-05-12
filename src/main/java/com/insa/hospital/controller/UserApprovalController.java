package com.insa.hospital.controller;

import com.insa.hospital.dto.PendingEmployeeResponse;
import com.insa.hospital.security.JwtContextHolder;
import com.insa.hospital.service.EmployeeApprovalService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin endpoints for reviewing self-registered employee accounts.
 */
@RestController
@RequestMapping("/api/users")
public class UserApprovalController {

    private final EmployeeApprovalService employeeApprovalService;

    public UserApprovalController(EmployeeApprovalService employeeApprovalService) {
        this.employeeApprovalService = employeeApprovalService;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<PendingEmployeeResponse>> getPendingEmployees(Authentication authentication) {
        boolean superadmin = isSuperadmin(authentication);
        return ResponseEntity.ok(
                employeeApprovalService.getPendingEmployees(
                        JwtContextHolder.getHospitalId(),
                        JwtContextHolder.getUserId(),
                        superadmin
                )
        );
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<PendingEmployeeResponse> approveEmployee(
            @PathVariable Long id,
            Authentication authentication
    ) {
        boolean superadmin = isSuperadmin(authentication);
        PendingEmployeeResponse approved = employeeApprovalService.approveEmployee(
                id,
                JwtContextHolder.getHospitalId(),
                JwtContextHolder.getUserId(),
                superadmin
        );
        return ResponseEntity.ok(approved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> rejectEmployee(
            @PathVariable Long id,
            Authentication authentication
    ) {
        boolean superadmin = isSuperadmin(authentication);
        employeeApprovalService.rejectEmployee(
                id,
                JwtContextHolder.getHospitalId(),
                JwtContextHolder.getUserId(),
                superadmin
        );
        return ResponseEntity.noContent().build();
    }

    private boolean isSuperadmin(Authentication authentication) {
        requireAdmin(authentication);
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_superadmin".equals(authority.getAuthority()));
    }

    private void requireAdmin(Authentication authentication) {
        boolean admin = authentication != null
                && authentication.getAuthorities().stream().anyMatch(authority ->
                "ROLE_admin".equals(authority.getAuthority())
                        || "ROLE_superadmin".equals(authority.getAuthority()));

        if (!admin) {
            throw new AccessDeniedException("Only administrators can manage employee approvals.");
        }
    }
}
