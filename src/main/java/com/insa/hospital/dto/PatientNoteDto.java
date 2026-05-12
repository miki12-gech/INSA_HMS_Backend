package com.insa.hospital.dto;

import lombok.Data;

@Data
public class PatientNoteDto {
    private String id;
    private String patientId;
    private String title;
    private String description;
    private String patientName;
    private String imgUrl;
    private String date;
    private String registrationTime;
    private String status;
    private String dateString;
    private String datetimeString;
    private String doctorName;
    private String hospitalId;
    private String doctorId;
}
