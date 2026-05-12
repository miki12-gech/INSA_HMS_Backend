package com.insa.hospital.dto;

/**
 * Row payload for the admin employee-verification queue.
 */
public record PendingEmployeeResponse(
        Long id,
        String name,
        String email,
        String phone,
        String insaIdCardNumber,
        Long registrationDate
) {}
