package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity mapping the legacy `report` table.
 *
 * Column mapping from nhospital1 SQL dump (lines 1821–1830).
 * ALL 8 columns mapped 1:1 — DO NOT add, rename, or remove columns.
 *
 * Key legacy observations:
 *  - report_type : 'birth' | 'operation' | 'expire' | other string values
 *  - patient     : varchar ID → patient.id (stored as String)
 *  - doctor      : varchar ID → doctor.id (stored as String)
 *  - date        : Unix epoch string
 *  - add_date    : "MM/DD/YY" format
 *  - description : up to 500 chars of free text
 *
 * RULE: ALL numeric FK references are varchar. No @ManyToOne/@JoinColumn.
 */
@Entity
@Table(name = "report")
@Getter
@Setter
@NoArgsConstructor
public class MedicalReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /** Type of report: 'birth' | 'operation' | 'expire' etc. */
    @Column(name = "report_type", length = 100)
    private String reportType;

    /** References patient.id as a string (legacy no-FK convention). */
    @Column(name = "patient", length = 100)
    private String patient;

    /** Free text description of the report. */
    @Column(name = "description", length = 500)
    private String description;

    /** References doctor.id as a string. */
    @Column(name = "doctor", length = 100)
    private String doctor;

    /** Unix epoch string — the report date. */
    @Column(name = "date", length = 100)
    private String date;

    /** Registration date in legacy format: MM/DD/YY. */
    @Column(name = "add_date", length = 100)
    private String addDate;

    /** Multi-tenant hospital identifier. */
    @Column(name = "hospital_id", length = 100)
    private String hospitalId;
}
