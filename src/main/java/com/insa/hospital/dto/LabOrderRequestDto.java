package com.insa.hospital.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LabOrderRequestDto {
    private String patientId;
    private String doctorId;
    private String categoryName;
    private String hospitalId;
    
    // Optional caching fields to match legacy UI constraints
    private String patientName;
    private String patientPhone;
    private String patientAddress;
    private String doctorName;
}
