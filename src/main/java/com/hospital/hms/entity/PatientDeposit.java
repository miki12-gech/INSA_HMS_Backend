package com.hospital.hms.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "patient_deposit")
public class PatientDeposit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient")
    private String patient;

    @Column(name = "payment_id")
    private String paymentId;

    @Column(name = "date")
    private String date;

    @Column(name = "deposited_amount")
    private String depositedAmount;

    @Column(name = "amount_received_id")
    private String amountReceivedId;

    @Column(name = "deposit_type")
    private String depositType;

    @Column(name = "gateway")
    private String gateway;

    @Column(name = "user")
    private String user;

    @Column(name = "hospital_id")
    private String hospitalId;
}
