package com.hospital.hms.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category")
    private String category;

    @Column(name = "patient")
    private String patient;

    @Column(name = "doctor")
    private String doctor;

    @Column(name = "date")
    private String date;

    @Column(name = "amount")
    private String amount;

    @Column(name = "vat", nullable = false, columnDefinition = "varchar(100) default '0'")
    private String vat;

    @Column(name = "x_ray")
    private String xRay;

    @Column(name = "flat_vat")
    private String flatVat;

    @Column(name = "discount", nullable = false, columnDefinition = "varchar(100) default '0'")
    private String discount;

    @Column(name = "flat_discount")
    private String flatDiscount;

    @Column(name = "gross_total")
    private String grossTotal;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "hospital_amount")
    private String hospitalAmount;

    @Column(name = "doctor_amount")
    private String doctorAmount;

    @Column(name = "category_amount", length = 1000)
    private String categoryAmount;

    @Column(name = "category_name", length = 1000)
    private String categoryName;

    @Column(name = "amount_received")
    private String amountReceived;

    @Column(name = "deposit_type")
    private String depositType;

    @Column(name = "status")
    private String status;

    @Column(name = "user")
    private String user;

    @Column(name = "patient_name")
    private String patientName;

    @Column(name = "patient_phone")
    private String patientPhone;

    @Column(name = "patient_address")
    private String patientAddress;

    @Column(name = "doctor_name")
    private String doctorName;

    @Column(name = "date_string")
    private String dateString;

    @Column(name = "hospital_id")
    private String hospitalId;
}
