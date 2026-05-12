package com.insa.hospital.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequestDto {
    private String type; // maps to report_type
    private String description;
    private String patient;
    private String doctor;
    private String date;
}
