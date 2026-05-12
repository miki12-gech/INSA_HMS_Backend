package com.hospital.hms.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "patient")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id")
    private String patientId;

    @Column(name = "name")
    private String name;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "sex")
    private String sex;

    @Column(name = "age")
    private String age;

    @Column(name = "add_date")
    private String addDate;

    @Column(name = "registration_time")
    private String registrationTime;

    @Column(name = "how_added")
    private String howAdded;

    @Column(name = "doctor")
    private String doctor;

    @Column(name = "ion_user_id")
    private String ionUserId;
    
    @Column(name = "hospital_id")
    private String hospitalId;
}
