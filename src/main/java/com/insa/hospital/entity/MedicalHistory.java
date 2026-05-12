package com.insa.hospital.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "medical_history")
@Getter
@Setter
@NoArgsConstructor
public class MedicalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "patient_id", length = 100)
    private String patientId;

    @Column(name = "title", length = 100)
    private String title;

    @Column(name = "description", length = 10000)
    private String description;

    @Column(name = "patient_name", length = 100)
    private String patientName;

    @Column(name = "patient_address", length = 500)
    private String patientAddress;

    @Column(name = "patient_phone", length = 100)
    private String patientPhone;

    @Column(name = "img_url", length = 500)
    private String imgUrl;

    @Column(name = "date", length = 100)
    private String date;

    @Column(name = "registration_time", length = 100)
    private String registrationTime;

    @Column(name = "hospital_id", length = 100)
    private String hospitalId;

    @Column(name = "diagnosis_category", length = 200)
    private String diagnosisCategory;
}
