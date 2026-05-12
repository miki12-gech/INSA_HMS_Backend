package com.hospital.hms.dto;

import lombok.Data;

@Data
public class PaymentDto {
    private Long id;
    private String category;
    private String patient;
    private String doctor;
    private String date;
    private String amount;
    private String vat;
    private String xRay;
    private String flatVat;
    private String discount;
    private String flatDiscount;
    private String grossTotal;
    private String remarks;
    private String hospitalAmount;
    private String doctorAmount;
    private String categoryAmount;
    private String categoryName;
    private String amountReceived;
    private String depositType;
    private String status;
    private String user;
    private String patientName;
    private String patientPhone;
    private String patientAddress;
    private String doctorName;
    private String dateString;
    private String hospitalId;
}
