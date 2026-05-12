package com.insa.hospital.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new Referral Request.
 *
 * Used by both entry points:
 *   1. Clinical — Doctor submits during patient examination.
 *   2. Self-Service — Employee submits for themselves or a family member.
 *
 * The `requestedByRole` and `requestedByUserId` are resolved server-side
 * from the JWT context and the caller's authority, but `requestedByRole`
 * is also accepted in the payload so the frontend can explicitly declare
 * the workflow origin.
 */
public record ReferralCreationDto(

    /** The patient being referred (legacy 6-digit ID or PK as string). */
    @NotBlank(message = "Patient ID is required")
    @Size(max = 100)
    String patientId,

    /**
     * Who is making the request: "DOCTOR" or "EMPLOYEE".
     * Validated against the caller's actual role on the server side.
     */
    @NotNull(message = "Requested-by role is required")
    String requestedByRole,

    /** Name of the external hospital the patient is being referred to. */
    @NotBlank(message = "Destination hospital is required")
    @Size(max = 500)
    String destinationHospital,

    /** Reason for the referral (clinical or administrative). */
    String reasonForReferral,

    /** Optional clinical/diagnostic notes from the Doctor. */
    String clinicalNotes

) {}
