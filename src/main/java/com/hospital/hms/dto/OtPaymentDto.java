package com.hospital.hms.dto;

import lombok.Data;

@Data
public class OtPaymentDto {
    private Long id;
    private String patient;
    private String doctorCS;
    private String doctorAS1;
    private String doctorAS2;
    private String doctorAnaes;
    private String nOo;
    private String cSf;
    private String aSf1;
    private String aSf2;
    private String anaesF;
    private String otCharge;
    private String cabRent;
    private String seatRent;
    private String others;
    private String discount;
    private String date;
    private String amount;
    private String doctorFees;
    private String hospitalFees;
    private String grossTotal;
    private String flatDiscount;
    private String amountReceived;
    private String status;
    private String user;
    private String hospitalId;
}
