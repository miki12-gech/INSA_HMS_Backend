package com.insa.hospital.controller;

import com.insa.hospital.dto.DashboardSummaryResponseDto;
import com.insa.hospital.security.JwtContextHolder;
import com.insa.hospital.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller — Admin/Doctor Dashboard endpoints.
 *
 * Base URL: /api/dashboard
 *
 * Endpoints:
 *   GET /api/dashboard/summary   → Full dashboard metrics snapshot
 *
 * All endpoints require a valid JWT. The hospital_id is resolved from
 * the token claims — no cross-hospital data leakage possible.
 *
 * Access: all authenticated roles (each role sees the same summary
 * scoped to their hospital — matches legacy behaviour where all roles
 * see the same dashboard header stats).
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @Autowired
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * GET /api/dashboard/summary
     *
     * Returns a snapshot of today's key performance metrics:
     * - Total & today's new patients
     * - Today's & pending appointments
     * - Today's revenue, unpaid invoice count
     * - Pending lab requests
     * - last 5 patients registered (activity feed)
     * - last 5 payments (revenue feed)
     *
     * Response is always fresh (no caching) — scoped to the requesting
     * user's hospital via JWT claims.
     */
    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DashboardSummaryResponseDto> getSummary() {
        String hospitalId = JwtContextHolder.getHospitalId();
        return ResponseEntity.ok(dashboardService.getDashboardSummary(hospitalId));
    }
}
