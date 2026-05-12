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
@Table(name = "patient_note")
@Getter
@Setter
@NoArgsConstructor
public class PatientNote {

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

    @Column(name = "img_url", length = 500)
    private String imgUrl;

    @Column(name = "date", length = 100)
    private String date;

    @Column(name = "registration_time", length = 100)
    private String registrationTime;

    @Column(name = "status", length = 100)
    private String status;

    @Column(name = "date_string", length = 100)
    private String dateString;

    @Column(name = "datetime_string", length = 100)
    private String datetimeString;

    @Column(name = "doctor_name", length = 100)
    private String doctorName;

    @Column(name = "hospital_id", length = 100)
    private String hospitalId;

    @Column(name = "doctor_id", length = 100)
    private String doctorId;
}
