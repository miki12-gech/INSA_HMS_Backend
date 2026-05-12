package com.insa.hospital.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating or updating a patient.
 *
 * Design:
 *  - Accepts 'age' (integer) OR 'birthdate' (String "DD-MM-YYYY").
 *  - If only 'age' is provided, the service auto-calculates 'birthdate'
 *    (Receptionist-requested feature: type age → system fills birthdate).
 *  - If both are provided, 'birthdate' takes precedence.
 *  - doctor, ion_user_id, hospital_id are resolved server-side from context.
 */
public record PatientRequestDto(

    @NotBlank(message = "Patient name is required")
    @Size(max = 100)
    String name,

    @Size(max = 1000)
    String email,

    @Size(max = 100)
    String phone,

    @Size(max = 100)
    String address,

    /** "Male" or "Female" */
    @Size(max = 100)
    String sex,

    /**
     * Birthdate in format "DD-MM-YYYY".
     * Optional — can be derived from 'age' if not provided.
     */
    @Size(max = 100)
    String birthdate,

    /**
     * Age in years (integer as String, matching legacy storage).
     * If birthdate is absent, the service calculates birthdate from this.
     */
    @Size(max = 100)
    String age,

    /** e.g. "A+", "B+", "O+", "AB-" */
    @Size(max = 100)
    String bloodgroup,

    /** e.g. "member", "Family" */
    @Size(max = 50)
    String membershiptype,

    /** Department ID — nullable integer. */
    Integer depId,

    /** Allergy notes — may contain plain text (frontend does NOT send HTML here). */
    @Size(max = 2000)
    String allergynote,

    /** Comma-separated doctor IDs (e.g. "149,151"). Optional. */
    @Size(max = 100)
    String doctor,

    /** Profile image URL if uploaded externally. */
    @Size(max = 100)
    String imgUrl

) {}
