package com.insa.hospital.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO for creating a prescription.
 *
 * Accepts the main prescription details PLUS a structured list of
 * medicine items. The service layer serializes the medicine list
 * into the legacy delimited string format before saving:
 *   medicineId***dosage***frequency***duration***instructions###...
 *
 * hospitalId and doctor are resolved from JWT context server-side.
 */
public record PrescriptionRequestDto(

    /** References patient.id. Required. */
    @NotBlank(message = "Patient is required")
    @Size(max = 100)
    String patient,

    /**
     * Appointment date or visit date as Unix epoch string.
     * If null/blank, server auto-sets to current epoch.
     */
    @Size(max = 100)
    String date,

    /** Chief complaint / symptom — HTML allowed (rich text). */
    @Size(max = 100)
    String symptom,

    /** Doctor's note — HTML allowed (rich text). */
    @Size(max = 1000)
    String note,

    /** Doctor's advice/instructions. */
    @Size(max = 1000)
    String advice,

    /** Prescription validity (e.g. "7 days", "1 month"). */
    @Size(max = 100)
    String validity,

    /**
     * Structured list of medicine items for this prescription.
     * The service serializes these into the legacy delimited string.
     * Can be null or empty (prescription with no medicines — note-only).
     */
    List<@Valid PrescriptionMedicineItemDto> medicines

) {

    /**
     * Represents ONE medicine entry in the prescription.
     * Maps to: medicineId***dosage***frequency***duration***instructions
     */
    public record PrescriptionMedicineItemDto(

        /** medicine.id */
        @NotNull(message = "Medicine ID is required")
        Integer medicineId,

        /** Dosage amount (e.g. "100mg", "1"). */
        @NotBlank(message = "Dosage is required")
        @Size(max = 100)
        String dosage,

        /** Frequency (e.g. "1+0+1", "BID", "TID"). */
        @Size(max = 100)
        String frequency,

        /** Duration (e.g. "7 days", "2 weeks", "4"). */
        @Size(max = 100)
        String duration,

        /** Instructions (e.g. "after lunch", "before food"). */
        @Size(max = 100)
        String instructions

    ) {}
}
