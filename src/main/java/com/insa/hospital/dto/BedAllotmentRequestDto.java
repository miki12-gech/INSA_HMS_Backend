package com.insa.hospital.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request DTO to allot (assign) a bed to a patient. */
public record BedAllotmentRequestDto(

    /** patient.id */
    @NotBlank(message = "Patient is required")
    @Size(max = 100)
    String patient,

    /**
     * Composite bed identifier (bed.bed_id), e.g. 'Icu-1'.
     * Used to look up the correct Bed record.
     */
    @NotBlank(message = "Bed ID is required")
    @Size(max = 100)
    String bedId

) {}
