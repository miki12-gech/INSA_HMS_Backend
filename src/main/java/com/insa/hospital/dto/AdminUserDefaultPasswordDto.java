package com.insa.hospital.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Superadmin-managed default password for staff provisioning and password resets.
 */
public record AdminUserDefaultPasswordDto(
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 255, message = "Password must be between 6 and 255 characters")
    String password
) {}
