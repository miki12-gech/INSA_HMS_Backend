package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "lab")
@Getter
@Setter
public class Lab {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_name")
    private String categoryName;

    @Column(columnDefinition = "TEXT")
    private String report;

    private String patient;
    private String date;
    private String doctor;
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
