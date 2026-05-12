package com.hospital.hms.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "appointment")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient")
    private String patient;

    @Column(name = "doctor")
    private String doctor;

    @Column(name = "date")
    private String date;

    @Column(name = "time_slot")
    private String timeSlot;

    @Column(name = "s_time")
    private String sTime;

    @Column(name = "e_time")
    private String eTime;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "add_date")
    private String addDate;

    @Column(name = "registration_time")
    private String registrationTime;

    @Column(name = "s_time_key")
    private String sTimeKey;

    @Column(name = "status")
    private String status;

    @Column(name = "user")
    private String user;

    @Column(name = "request")
    private String request;

    @Column(name = "hospital_id")
    private String hospitalId;

    @Column(name = "category")
    private String category;
}
