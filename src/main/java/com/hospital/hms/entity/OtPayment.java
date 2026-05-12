package com.hospital.hms.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ot_payment")
public class OtPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient")
    private String patient;

    @Column(name = "doctor_c_s")
    private String doctorCS;

    @Column(name = "doctor_a_s_1")
    private String doctorAS1;

    @Column(name = "doctor_a_s_2")
    private String doctorAS2;

    @Column(name = "doctor_anaes")
    private String doctorAnaes;

    @Column(name = "n_o_o")
    private String nOo;

    @Column(name = "c_s_f")
    private String cSf;

    @Column(name = "a_s_f_1")
    private String aSf1;

    @Column(name = "a_s_f_2", length = 11)
    private String aSf2;

    @Column(name = "anaes_f")
    private String anaesF;

    @Column(name = "ot_charge")
    private String otCharge;

    @Column(name = "cab_rent")
    private String cabRent;

    @Column(name = "seat_rent")
    private String seatRent;

    @Column(name = "others")
    private String others;

    @Column(name = "discount")
    private String discount;

    @Column(name = "date")
    private String date;

    @Column(name = "amount")
    private String amount;

    @Column(name = "doctor_fees")
    private String doctorFees;

    @Column(name = "hospital_fees")
    private String hospitalFees;

    @Column(name = "gross_total")
    private String grossTotal;

    @Column(name = "flat_discount")
    private String flatDiscount;

    @Column(name = "amount_received")
    private String amountReceived;

    @Column(name = "status")
    private String status;

    @Column(name = "user")
    private String user;

    @Column(name = "hospital_id")
    private String hospitalId;
}
