package com.hospital.hms.dto;

import lombok.Data;

@Data
public class PatientDepositDto {
    private Long id;
    private String patient;
    private String paymentId;
    private String date;
    private String depositedAmount;
    private String amountReceivedId;
    private String depositType;
    private String gateway;
    private String user;
    private String hospitalId;
}
