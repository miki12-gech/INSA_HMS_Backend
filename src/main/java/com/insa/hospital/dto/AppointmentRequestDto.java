package com.insa.hospital.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for booking or updating an appointment.
 *
 * Date/time notes (agent.md §6):
 *  - 'date' must be a Unix epoch string (e.g. "1640041200")
 *  - 'addDate' will be auto-set by the service to today in "MM/DD/YY" format
 *  - 'registrationTime' will be auto-set to current Unix epoch
 *  - status defaults to 'Pending Confirmation' if not provided
 */
public record AppointmentRequestDto(

    /** References patient.id (varchar). Required. */
    @NotBlank(message = "Patient is required")
    @Size(max = 100)
    String patient,

    /** References doctor.id (varchar). Required. */
    @NotBlank(message = "Doctor is required")
    @Size(max = 100)
    String doctor,

    /**
     * Appointment date as Unix epoch string (e.g. "1640041200").
     * Frontend should convert selected date to epoch before sending.
     */
    @NotBlank(message = "Date is required")
    @Size(max = 100)
    String date,

    /** Time slot label (e.g. "08:30 AM To 08:45 AM" or "Not Selected"). */
    @Size(max = 100)
    String timeSlot,

    /** Start time string (e.g. "08:30 AM"). */
    @Size(max = 100)
    String sTime,

    /** End time string (e.g. "08:45 AM"). */
    @Size(max = 100)
    String eTime,

    /** Slot key index from time_slot table. */
    @Size(max = 100)
    String sTimeKey,

    /** Doctor's remarks or notes about the appointment. */
    @Size(max = 500)
    String remarks,

    /**
     * Status: 'Pending Confirmation' | 'Confirmed' | 'Treated'.
     * Defaults to 'Pending Confirmation' if omitted.
     */
    @Size(max = 100)
    String status,

    /** Legacy request/referral ID. */
    @Size(max = 100)
    String request,

    /** Patient category: 'Out Patient' | 'In Patient' | 'Emergency' | 'ICU'. */
    @Size(max = 50)
    String category,

    // ─── Legacy new patient dynamic payload properties ───

    @Size(max = 100)
    String p_name,

    @Size(max = 100)
    String p_email,

    @Size(max = 100)
    String p_phone,

    @Size(max = 10)
    String p_age,

    @Size(max = 50)
    String p_gender,

    /** 'Yes' or empty, signals if SMS is triggered on creation */
    @Size(max = 10)
    String sms

) {}
