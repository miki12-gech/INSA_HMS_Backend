package com.insa.hospital.dto;

import com.insa.hospital.entity.Prescription;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Response DTO for prescriptions.
 *
 * Parses the legacy delimited medicine string back into a structured list
 * so the frontend receives clean JSON — no raw delimited strings.
 *
 * Also carries resolved `patientName` and `doctorName` when available
 * (set by the controller layer after name resolution from repositories).
 *
 * Examples of the raw medicine field:
 *   Single:   "2866***100mg***1+0+1***2***after lunch"
 *   Multiple: "2866***100mg***1+0+1***2***after lunch###2868***100mg***1+0+1***4***after lunch"
 */
public record PrescriptionResponseDto(
    Integer id,
    String date,
    String patient,
    String doctor,
    /** Resolved patient name — populated by controller after DB lookup. */
    String patientName,
    /** Resolved doctor name — populated by controller after DB lookup. */
    String doctorName,
    String symptom,
    String advice,
    String state,
    String dd,
    /** Raw legacy delimited string — also exposed for backward compat. */
    String medicineRaw,
    /** Parsed structured list — preferred for frontend consumption. */
    List<ParsedMedicineItem> medicines,
    String validity,
    String note,
    String hospitalId
) {

    public record ParsedMedicineItem(
        String medicineId,
        String dosage,
        String frequency,
        String duration,
        String instructions
    ) {}

    /**
     * Creates DTO from entity WITHOUT resolved names.
     * Names default to the raw ID values.
     */
    public static PrescriptionResponseDto from(Prescription p) {
        List<ParsedMedicineItem> parsed = parseMedicines(p.getMedicine());
        return new PrescriptionResponseDto(
            p.getId(),
            p.getDate(),
            p.getPatient(),
            p.getDoctor(),
            null,   // patientName — will be resolved by controller
            null,   // doctorName  — will be resolved by controller
            p.getSymptom(),
            p.getAdvice(),
            p.getState(),
            p.getDd(),
            p.getMedicine(),
            parsed,
            p.getValidity(),
            p.getNote(),
            p.getHospitalId()
        );
    }

    /**
     * Creates DTO from entity WITH resolved names.
     */
    public static PrescriptionResponseDto from(Prescription p, String patientName, String doctorName) {
        List<ParsedMedicineItem> parsed = parseMedicines(p.getMedicine());
        return new PrescriptionResponseDto(
            p.getId(),
            p.getDate(),
            p.getPatient(),
            p.getDoctor(),
            patientName,
            doctorName,
            p.getSymptom(),
            p.getAdvice(),
            p.getState(),
            p.getDd(),
            p.getMedicine(),
            parsed,
            p.getValidity(),
            p.getNote(),
            p.getHospitalId()
        );
    }

    /**
     * Parses "2866***100mg***1+0+1***2***after lunch###2868***..." into list.
     * Returns empty list if medicine string is null/blank.
     */
    static List<ParsedMedicineItem> parseMedicines(String raw) {
        if (raw == null || raw.isBlank()) return Collections.emptyList();
        return Arrays.stream(raw.split(Prescription.MEDICINE_SEPARATOR, -1))
            .filter(s -> !s.isBlank())
            .map(entry -> {
                String[] parts = entry.split("\\*\\*\\*", -1);
                return new ParsedMedicineItem(
                    parts.length > 0 ? parts[0].trim() : "",
                    parts.length > 1 ? parts[1].trim() : "",
                    parts.length > 2 ? parts[2].trim() : "",
                    parts.length > 3 ? parts[3].trim() : "",
                    parts.length > 4 ? parts[4].trim() : ""
                );
            })
            .collect(Collectors.toList());
    }
}
