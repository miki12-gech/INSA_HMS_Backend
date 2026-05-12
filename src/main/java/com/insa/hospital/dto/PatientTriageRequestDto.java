package com.insa.hospital.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for recording vital signs at triage.
 * Date fields (date, date_string, hospital_id) are set server-side.
 * patient_name is auto-resolved from the patient table.
 */
public record PatientTriageRequestDto(

    /** References patient.id (varchar). Required. */
    @NotBlank(message = "Patient is required")
    @Size(max = 100)
    String patient,

    /** Chief complaint or visit reason. */
    @Size(max = 100)
    String title,

    /** Blood pressure (e.g. "120/80"). */
    @Size(max = 100)
    String blood_pressure,

    /** Heart rate in BPM (maps to legacy column heatBeat). */
    @Size(max = 100)
    String heatBeat,

    /** SpO2 oxygen saturation %. */
    @Size(max = 100)
    String oxygenSaturation,

    /** Blood sugar level (maps to legacy column sugerlevel — typo preserved). */
    @Size(max = 100)
    String sugerlevel,

    /** Height in centimeters. */
    @Size(max = 100)
    String height,

    /** Weight in kg. */
    @Size(max = 100)
    String weight,

    /** Body temperature. */
    @Size(max = 100)
    String temperature,

    /** Respiratory rate (maps to legacy column resparatoryRate — typo preserved). */
    @Size(max = 100)
    String resparatoryRate,

    /** Optional URL to an uploaded triage document or image. */
    @Size(max = 1000)
    String url

) {}
