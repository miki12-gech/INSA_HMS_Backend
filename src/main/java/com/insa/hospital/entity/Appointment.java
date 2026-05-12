package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity mapping the legacy `appointment` table.
 *
 * Column mapping from nhospital1 SQL dump (lines 84–101).
 * ALL 16 columns mapped 1:1 — DO NOT add, rename, or remove columns.
 *
 * Key legacy observations from actual data rows (lines 108–122):
 *  - patient          : varchar ID → patient.id (stored as String)
 *  - doctor           : varchar ID → doctor.id (stored as String)
 *  - date             : Unix epoch string (e.g. "1640041200")
 *  - add_date         : "MM/DD/YY" format (e.g. "12/21/21")
 *  - registration_time: Unix epoch string (when appointment was booked)
 *  - s_time           : Start time as string (e.g. "08:30 AM" or "Not Selected")
 *  - e_time           : End time as string
 *  - s_time_key       : Slot index key (e.g. "102", "243") — maps to time_slot
 *  - time_slot        : Slot label (e.g. "08:30 AM To 08:45 AM", "Not Selected", "0")
 *  - status           : 'Pending Confirmation' | 'Confirmed' | 'Treated'
 *  - user             : users.id who booked this (varchar)
 *  - request          : legacy request/referral field
 *  - category         : 'Out Patient' | 'In Patient' | 'Emergency' | 'ICU' | '0'
 *
 * RULE (agent.md §7): ALL numeric FK references are varchar. No @ManyToOne/@JoinColumn.
 */
@Entity
@Table(name = "appointment")
@Getter
@Setter
@NoArgsConstructor
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /** References patient.id as a string (legacy no-FK convention). */
    @Column(name = "patient", length = 100)
    private String patient;

    /** References doctor.id as a string. */
    @Column(name = "doctor", length = 100)
    private String doctor;

    /** Unix epoch string — the appointment date. e.g. "1640041200". */
    @Column(name = "date", length = 100)
    private String date;

    /** Full slot label, e.g. "08:30 AM To 08:45 AM" or "Not Selected". */
    @Column(name = "time_slot", length = 100)
    private String timeSlot;

    /** Start time string, e.g. "08:30 AM" or "Not Selected". */
    @Column(name = "s_time", length = 100)
    private String sTime;

    /** End time string. */
    @Column(name = "e_time", length = 100)
    private String eTime;

    @Column(name = "remarks", length = 500)
    private String remarks;

    /** Booking date in "MM/DD/YY" format. */
    @Column(name = "add_date", length = 100)
    private String addDate;

    /** Unix epoch string — when appointment was recorded in the system. */
    @Column(name = "registration_time", length = 100)
    private String registrationTime;

    /** Numeric key of the selected time slot from time_slot table. */
    @Column(name = "s_time_key", length = 100)
    private String sTimeKey;

    /** 'Pending Confirmation' | 'Confirmed' | 'Treated' */
    @Column(name = "status", length = 100)
    private String status;

    /** users.id of the staff member who booked this appointment. */
    @Column(name = "\"user\"", length = 100)
    private String user;

    /** Legacy request/referral field. */
    @Column(name = "request", length = 100)
    private String request;

    /** Multi-tenant hospital identifier. */
    @Column(name = "hospital_id", length = 100)
    private String hospitalId;

    /** Patient category: 'Out Patient' | 'In Patient' | 'Emergency' | 'ICU' */
    @Column(name = "category", length = 50)
    private String category;
}
