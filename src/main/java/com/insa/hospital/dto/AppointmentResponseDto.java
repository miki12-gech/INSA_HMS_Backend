package com.insa.hospital.dto;

import com.insa.hospital.entity.Appointment;

/**
 * Response DTO for appointment data.
 *
 * Enriched with patientName and doctorName — resolved at service layer
 * from the patient and doctor tables so the frontend never has to make
 * additional calls just to display a name.
 */
public record AppointmentResponseDto(
    Integer id,
    String patient,           // patient.id (raw FK string)
    String patientName,       // ENRICHED: resolved from patient table
    String doctor,            // doctor.id (raw FK string)
    String doctorName,        // ENRICHED: resolved from doctor table
    String doctorDepartment,  // ENRICHED: resolved from doctor table
    String date,              // Unix epoch string
    String timeSlot,
    String sTime,
    String eTime,
    String sTimeKey,
    String remarks,
    String addDate,
    String registrationTime,
    String status,
    String user,
    String request,
    String hospitalId,
    String category
) {
    /**
     * Base factory (no enrichment — use when patient/doctor names are not available).
     * The service layer uses the overloaded version with names.
     */
    public static AppointmentResponseDto from(Appointment a) {
        return from(a, null, null, null);
    }

    /**
     * Enriched factory — service passes resolved names.
     */
    public static AppointmentResponseDto from(
            Appointment a,
            String patientName,
            String doctorName,
            String doctorDepartment) {

        return new AppointmentResponseDto(
            a.getId(),
            a.getPatient(),
            patientName,
            a.getDoctor(),
            doctorName,
            doctorDepartment,
            a.getDate(),
            a.getTimeSlot(),
            a.getSTime(),
            a.getETime(),
            a.getSTimeKey(),
            a.getRemarks(),
            a.getAddDate(),
            a.getRegistrationTime(),
            a.getStatus(),
            a.getUser(),
            a.getRequest(),
            a.getHospitalId(),
            a.getCategory()
        );
    }
}
