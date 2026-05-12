package com.insa.hospital.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ServiceChargeBulkCreateRequestDto(
        @NotBlank(message = "Patient ID is required")
        @Size(max = 100)
        String patientId,

        @Size(max = 255)
        String patientName,

        @Size(max = 100)
        String visitId,

        @Size(max = 100)
        String responsibleDoctorId,

        @Size(max = 255)
        String responsibleDoctorName,

        @Size(max = 1000)
        String notes,

        @NotEmpty(message = "At least one service item is required")
        List<@Valid ServiceChargeItemDto> items
) {
    public record ServiceChargeItemDto(
            @NotNull(message = "Service catalog ID is required")
            Integer serviceCatalogId,

            @Positive(message = "Quantity must be greater than zero")
            Integer quantity,

            @Size(max = 100)
            String unitPrice
    ) {
    }
}
