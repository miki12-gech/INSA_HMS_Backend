package com.insa.hospital.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MachinePayloadDto {
    // The generic LIS machine JSON schema
    private String patientId;
    private String testType; // e.g. "CBC", "Urinalysis"
    private String timestamp; // Machine ISO time
    private String machineId; // Dymind LIS Identifier
    
    // Key/Value pair of test parameters (e.g. "WBC": "5.4", "RBC": "4.1")
    private Map<String, String> results; 
}
