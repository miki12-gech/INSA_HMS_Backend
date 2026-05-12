package com.insa.hospital.dto;

public record LabRequestDto(
    Long id,
    String report,
    String patient,
    String date,
    String doctor,
    String status,
    String templet_id,
    
    // Add new patient explicitly fields
    String p_name,
    String p_email,
    String p_phone,
    String p_age,
    String p_gender,
    
    // Add new doctor explicitly fields
    String d_name,
    String d_email,
    String d_phone,
    
    String discount,
    String amount_received
) {}
