package com.insa.hospital.dto;

/**
 * Registration response for the employee approval workflow.
 */
public record EmployeeRegistrationResponse(
        Long id,
        String status,
        String message
) {}
