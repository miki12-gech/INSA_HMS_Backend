package com.insa.hospital.dto;

import lombok.Data;

@Data
public class MedicalHistoryResponseDto {
    private Integer id;
    private String patientId;
    private String patientName;
    private String patientPhone;
    private String patientAddress;
    private String date;
    private String title;
    private String description;
    private String diagnosisCategory;
    private String imgUrl;
    private String registrationTime;
}
