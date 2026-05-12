package com.insa.hospital.dto;

import lombok.Data;
import java.util.List;

@Data
public class MedicineIssueRequestDto {
    private String categoryId;
    private String receiverName;
    private String user;
    private String date;
    private String hospitalId;
    
    private List<MedicineIssueItemDto> items;

    @Data
    public static class MedicineIssueItemDto {
        private String medicineId;
        private Integer quantity;
    }
}
