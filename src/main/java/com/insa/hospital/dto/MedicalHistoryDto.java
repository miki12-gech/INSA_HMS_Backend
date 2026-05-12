package com.insa.hospital.dto;

import lombok.Data;

@Data
public class MedicalHistoryDto {
    private String id;
    private String patientId;
    private String title;
    private String description;
    private String patientName;
    private String patientAddress;
    private String patientPhone;
    private String imgUrl;
    private String date;
    private String registrationTime;
    private String hospitalId;
    private String diagnosisCategory;
}
