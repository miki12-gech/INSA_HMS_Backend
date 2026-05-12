package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "diagnostic_report")
public class LabResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "date")
    private String date;

    @Column(name = "invoice")
    private String invoice;

    @Column(name = "report", length = 10000)
    private String report;

    @Column(name = "status")
    private String status;

    @Column(name = "hospital_id")
    private String hospitalId;
}
