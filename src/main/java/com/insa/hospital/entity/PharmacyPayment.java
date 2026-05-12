package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "pharmacy_payment")
@Data
public class PharmacyPayment {

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

    @Column(name = "vat")
    private String vat = "0";

    @Column(name = "x_ray")
    private String xRay; // Maps to x_ray in DB

    @Column(name = "flat_vat")
    private String flatVat;

    @Column(name = "discount")
    private String discount = "0";

    @Column(name = "flat_discount")
    private String flatDiscount;

    @Column(name = "gross_total")
    private String grossTotal;

    @Column(name = "hospital_amount")
    private String hospitalAmount;

    @Column(name = "doctor_amount")
    private String doctorAmount;

    @Column(name = "category_amount")
    private String categoryAmount;

    @Column(name = "category_name")
    private String categoryName;

    @Column(name = "amount_received")
    private String amountReceived;

    @Column(name = "status")
    private String status;

    @Column(name = "hospital_id")
    private String hospitalId;
}
