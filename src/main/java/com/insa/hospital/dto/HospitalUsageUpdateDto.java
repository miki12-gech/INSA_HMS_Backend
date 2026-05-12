package com.insa.hospital.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record HospitalUsageUpdateDto(
        @NotBlank(message = "Usage status is required")
        @Pattern(regexp = "(?i)active|stopped", message = "Usage status must be Active or Stopped")
        String usageStatus
) {}
