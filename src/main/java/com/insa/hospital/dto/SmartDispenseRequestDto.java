package com.insa.hospital.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SmartDispenseRequestDto {
    private Integer prescriptionId;
    private String patientId;
    private String hospitalId;
    private List<MedicineDispenseItem> medicinesToDispense;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicineDispenseItem {
        private Integer medicineId;
        private Integer quantitySold;
    }
}
