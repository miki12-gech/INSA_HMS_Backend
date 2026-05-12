package com.insa.hospital.dto;

import lombok.Data;

@Data
public class PatientNoteResponseDto {
    private Integer id;
    private String patientId;
    private String patientName;
    private String date;
    private String title;
    private String description;
    private String dateString;
    private String datetimeString;
    private String doctorName;
    private String doctorId;
    private String status;
}
