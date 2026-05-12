package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity mapping the legacy `patient` table.
 *
 * Column mapping from nhospital1 SQL dump (lines 1195–1216).
 * ALL 19 columns mapped 1:1 — DO NOT add, rename, or remove columns.
 *
 * Key legacy format observations (from actual data rows):
 *  - birthdate       : "DD-MM-YYYY"  (e.g. "07-07-2019", "09-02-2000")
 *  - add_date        : "MM/DD/YY"    (e.g. "07/07/19", "12/20/21")
 *  - registration_time : Unix epoch string (e.g. "1562482338")
 *  - age             : String, sometimes empty ("") or null
 *  - doctor          : Comma-separated user IDs (e.g. ",149,151")
 *  - dep_id          : int(11), nullable — not varchar, stored as Integer
 */
@Entity
@Table(name = "patient")
@Getter
@Setter
@NoArgsConstructor
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "img_url", length = 100)
    private String imgUrl;

    @Column(name = "name", length = 100)
    private String name;

    /** Legacy column is varchar(1000) — large because it can hold multiple emails. */
    @Column(name = "email", length = 1000)
    private String email;

    /**
     * Comma-separated doctor IDs (e.g. ",149,151" or "150").
     * Mapped as String — do NOT convert to a collection.
     */
    @Column(name = "doctor", length = 100)
    private String doctor;

    @Column(name = "address", length = 100)
    private String address;

    @Column(name = "phone", length = 100)
    private String phone;

    /** "Male" or "Female" — stored as plain string. */
    @Column(name = "sex", length = 100)
    private String sex;

    /**
     * Date of birth in legacy format: DD-MM-YYYY.
     * Example: "07-07-2019", "09-02-2000".
     * Stored as String — never convert to LocalDate (breaks 1:1 parity).
     */
    @Column(name = "birthdate", length = 100)
    private String birthdate;

    /**
     * Age in years, stored as String.
     * May be empty string ("") or null in legacy records.
     * Calculated from birthdate when provided on registration.
     */
    @Column(name = "age", length = 100)
    private String age;

    /** e.g. "A+", "B+", "O+", "AB-" */
    @Column(name = "bloodgroup", length = 100)
    private String bloodgroup;

    /** References users.id — the logged-in ion_auth user for this patient. */
    @Column(name = "ion_user_id", length = 100)
    private String ionUserId;

    /**
     * Legacy 6-digit patient ID generated randomly at registration.
     * Example: "727265", "170108", "816499".
     */
    @Column(name = "patient_id", length = 100)
    private String patientId;

    /**
     * Registration date in legacy format: MM/DD/YY.
     * Example: "07/07/19", "12/20/21".
     */
    @Column(name = "add_date", length = 100)
    private String addDate;

    /** Unix epoch timestamp string of when the patient was registered. */
    @Column(name = "registration_time", length = 100)
    private String registrationTime;

    /** e.g. "from_appointment", "from_pos", "" */
    @Column(name = "how_added", length = 100)
    private String howAdded;

    /** Multi-tenant hospital identifier. */
    @Column(name = "hospital_id", length = 100)
    private String hospitalId;

    /** e.g. "member", "Family" */
    @Column(name = "membershiptype", length = 50)
    private String membershiptype;

    /**
     * Department ID (int(11), nullable).
     * Stored as Integer, NOT varchar — special case in this schema.
     */
    @Column(name = "dep_id")
    private Integer depId;

    /**
     * Allergy notes — may contain HTML from rich text editor.
     * Large field: varchar(2000).
     */
    @Column(name = "allergynote", length = 2000)
    private String allergynote;
}
