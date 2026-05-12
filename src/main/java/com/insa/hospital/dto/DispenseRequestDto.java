package com.insa.hospital.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DispenseRequestDto {
    private Integer medicineId;
    private String patientId;
    private Integer quantity;
    private String hospitalId;
}
