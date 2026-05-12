package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity mapping the legacy `prescription` table.
 *
 * Column mapping from nhospital1 SQL dump (lines 1742–1755).
 * ALL 12 columns mapped 1:1 — DO NOT add, rename, or remove columns.
 *
 * ══════════════════════════════════════════════════════════════
 *  CRITICAL LEGACY DESIGN: prescription.medicine column
 * ══════════════════════════════════════════════════════════════
 *  The `medicine` column is NOT a FK — it stores multiple
 *  prescriptions as a single delimited string:
 *
 *    Format (single medicine):
 *      medicineId***dosage***frequency***duration***instructions
 *
 *    Format (multiple medicines, separated by ###):
 *      2866***100mg***1+0+1***2***after lunch###2868***100mg***1+0+1***4***after lunch
 *
 *  This MUST be parsed and serialized in the service layer.
 *  The entity stores it as-is (String). Parsing is done in
 *  PrescriptionService using the delimiter constants.
 *
 * Additional observations:
 *  - patient : varchar ID → patient.id
 *  - doctor  : varchar ID → doctor.id (may be "0" if anonymous)
 *  - date    : Unix epoch string
 *  - symptom : raw HTML (rich text editor output)
 *  - note    : raw HTML (rich text editor output)
 * ══════════════════════════════════════════════════════════════
 */
@Entity
@Table(name = "prescription")
@Getter
@Setter
@NoArgsConstructor
public class Prescription {

    /** Delimiter separating individual medicine entries */
    public static final String MEDICINE_SEPARATOR = "###";
    /** Delimiter separating fields within one medicine entry */
    public static final String FIELD_SEPARATOR = "***";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /** Unix epoch string — prescription date. */
    @Column(name = "date", length = 100)
    private String date;

    /** References patient.id as string. */
    @Column(name = "patient", length = 100)
    private String patient;

    /** References doctor.id as string (may be "0"). */
    @Column(name = "doctor", length = 100)
    private String doctor;

    /** Chief complaint / symptom — raw HTML from rich text editor. */
    @Column(name = "symptom", length = 100)
    private String symptom;

    /** Doctor advice notes. */
    @Column(name = "advice", length = 1000)
    private String advice;

    /** Legacy state field (nullable). */
    @Column(name = "state", length = 100)
    private String state;

    /** Legacy dd field (nullable). */
    @Column(name = "dd", length = 100)
    private String dd;

    /**
     * Delimited medicine string.
     * Format: medicineId***dosage***frequency***duration***instructions
     * Multiple medicines separated by ###
     * Parse/serialize in PrescriptionService, never raw-build here.
     */
    @Column(name = "medicine", length = 1000)
    private String medicine;

    /** Prescription validity/expiry. */
    @Column(name = "validity", length = 100)
    private String validity;

    /** Doctor note — raw HTML from rich text editor. */
    @Column(name = "note", length = 1000)
    private String note;

    /** Multi-tenant hospital identifier. */
    @Column(name = "hospital_id", length = 100)
    private String hospitalId;
}
