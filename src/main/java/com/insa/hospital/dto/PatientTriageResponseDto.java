package com.insa.hospital.dto;

import com.insa.hospital.entity.PatientTriage;

/**
 * Response DTO for patient triage / vitals records.
 */
public record PatientTriageResponseDto(
    Integer id,
    String date,
    String dateString,
    String patient,
    String patientName,
    String title,
    String blood_pressure,
    String heatBeat,
    String oxygenSaturation,
    String sugerlevel,
    String height,
    String weight,
    String temperature,
    String resparatoryRate,
    String url,
    String hospitalId
) {
    public static PatientTriageResponseDto from(PatientTriage t) {
        return new PatientTriageResponseDto(
            t.getId(),
            t.getDate(),
            t.getDateString(),
            t.getPatient(),
            t.getPatientName(),
            t.getTitle(),
            t.getBloodPressure(),
            t.getHeatBeat(),
            t.getOxygenSaturation(),
            t.getSugerlevel(),
            t.getHeight(),
            t.getWeight(),
            t.getTemperature(),
            t.getResparatoryRate(),
            t.getUrl(),
            t.getHospitalId()
        );
    }
}
