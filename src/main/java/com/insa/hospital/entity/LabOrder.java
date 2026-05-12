package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "lab")
public class LabOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "category")
    private String category;

    @Column(name = "patient")
    private String patient;

    @Column(name = "doctor")
    private String doctor;

    @Column(name = "date")
    private String date;

    @Column(name = "category_name")
    private String categoryName;

    @Column(name = "report", length = 10000)
    private String report;

    @Column(name = "status")
    private String status;

    @Column(name = "\"user\"")
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
