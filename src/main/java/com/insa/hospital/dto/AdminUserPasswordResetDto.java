package com.insa.hospital.dto;

import jakarta.validation.constraints.Size;

/**
 * Request payload for superadmin password resets.
 *
 * If newPassword is omitted/blank, the service falls back to the default
 * temporary password "12345678".
 */
public record AdminUserPasswordResetDto(
    @Size(min = 6, max = 255, message = "Password must be between 6 and 255 characters")
    String newPassword
) {}
