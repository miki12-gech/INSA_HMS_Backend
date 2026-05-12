package com.insa.hospital.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * System Health Check Controller
 *
 * Provides a public endpoint to verify the API and database are operational.
 * This endpoint is intentionally excluded from JWT authentication so that
 * load balancers, monitoring tools, and the frontend can poll it freely.
 *
 * GET /api/health
 */
@RestController
@RequestMapping("/api")
public class SystemHealthController {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public SystemHealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Health check endpoint.
     *
     * @return JSON: { status, message, database, timestamp }
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("message", "Hospital API is running");
        response.put("timestamp", LocalDateTime.now().toString());

        // Verify database connectivity with a lightweight query
        String dbStatus;
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            dbStatus = (result != null && result == 1) ? "Connected" : "Degraded";
        } catch (Exception e) {
            dbStatus = "Disconnected — " + e.getMessage();
            response.put("status", "DOWN");
        }

        response.put("database", dbStatus);
        return ResponseEntity.ok(response);
    }
}
