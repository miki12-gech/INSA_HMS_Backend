package com.insa.hospital.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MedicineRequestDto(

    @NotBlank(message = "Medicine name is required")
    @Size(max = 100)
    String name,

    @Size(max = 100)
    String category,

    @Size(max = 50)
    String category1,

    @Size(max = 100)
    String price,

    @Size(max = 100)
    String sPrice,

    @Size(max = 100)
    String box,

    Integer quantity,

    @Size(max = 100)
    String generic,

    @Size(max = 100)
    String company,

    @Size(max = 100)
    String effects,

    /** Expiry date string (e.g. "28-02-2025"). */
    @Size(max = 70)
    String eDate,

    @Size(max = 50)
    String strength

) {}
