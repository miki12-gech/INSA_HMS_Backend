package com.insa.hospital.dto;

import lombok.Data;
import java.util.List;

@Data
public class PharmacyPosRequestDto {
    private String patient;
    private String guestName;
    private String guestPhone;
    private String doctor;
    private String date;
    private String xRay;
    private String amountReceived;
    private String discount; // flat discount amount or percentage
    private String hospitalId;
    
    private List<PosItemDto> items;

    @Data
    public static class PosItemDto {
        private String medicineId;
        private Integer quantity;
    }
}
