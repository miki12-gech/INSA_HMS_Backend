package com.insa.hospital.dto;

import lombok.Data;
import java.util.List;

@Data
public class PatientMedicalHistoryResponseDto {
    private String patientId;
    private List<PatientTriageResponseDto> vitals;
    private List<MedicalHistoryDto> pastDiagnoses;
    private List<PatientNoteDto> clinicalNotes;
    private List<PrescriptionResponseDto> prescriptions;
}
