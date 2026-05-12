package com.insa.hospital.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request payload for suspending or re-enabling a staff account.
 */
public record AdminUserStatusUpdateDto(
    @NotNull(message = "Active flag is required")
    Boolean active
) {}
