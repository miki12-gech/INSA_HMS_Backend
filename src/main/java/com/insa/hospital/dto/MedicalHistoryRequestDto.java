package com.insa.hospital.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MedicalHistoryRequestDto {
    @NotBlank(message = "Patient ID is required")
    private String patientId;

    private String date;
    private String title;
    private String description;
    private String diagnosisCategory;
}
