package com.insa.hospital.dto;

import java.util.List;

public record LabResponseDto(
    Long id,
    String report,
    String patient,
    String date,
    String doctor,
    String status,
    String user,
    String patient_name,
    String patient_phone,
    String patient_address,
    String doctor_name,
    String date_string,
    
    // Embedded template IDs
    List<String> template_ids
) {}
