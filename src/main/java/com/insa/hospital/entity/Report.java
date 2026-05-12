package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "report")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_type")
    private String reportType; // Ex: 'birth', 'operation', 'expire'

    @Column(columnDefinition = "VARCHAR(1000)")
    private String description;

    @Column(name = "patient")
    private String patient;

    @Column(name = "doctor")
    private String doctor;

    @Column(name = "date")
    private String date;

    @Column(name = "add_date")
    private String addDate;

    @Column(name = "hospital_id")
    private String hospitalId;
}
