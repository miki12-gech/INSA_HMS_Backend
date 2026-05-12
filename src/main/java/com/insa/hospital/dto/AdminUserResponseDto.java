package com.insa.hospital.dto;

/**
 * Superadmin staff management response row.
 */
public record AdminUserResponseDto(
    Long id,
    String name,
    String email,
    String phone,
    String role,
    boolean active,
    String hospitalId,
    String hospitalName,
    Integer departmentId,
    String departmentName,
    Long createdOn
) {}
