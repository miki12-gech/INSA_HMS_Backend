package com.insa.hospital.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PaymentCategoryUpsertDto(
        @NotBlank(message = "Service name is required")
        @Size(max = 100)
        String category,

        @Size(max = 100)
        String description,

        @Size(max = 100)
        String cPrice,

        @Size(max = 100)
        String type,

        Integer dCommission,
        Integer hCommission,

        @Size(max = 100)
        String serviceGroup,

        @Size(max = 100)
        String serviceRole,

        @Size(max = 100)
        String revenueTarget,

        Boolean active
) {
}
