package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity mapping the legacy `patient_triage` table.
 * 
 * Strict @Column(name="...") mappings are enforced below 
 * resulting from a deep scan of the legacy SQL schema string:
 * `id`, `date`, `patient`, `patient_name`, `bloodPressures`, `heatBeat`, 
 * `oxygenSaturation`, `sugerlevel`, `height`, `weight`, `temperature`,
 * `url`, `date_string`, `hospital_id`, `title`, `resparatoryRate`
 */
@Entity
@Table(name = "patient_triage")
@Getter
@Setter
@NoArgsConstructor
public class PatientTriage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "date", length = 100)
    private String date;

    @Column(name = "patient", length = 100)
    private String patient;

    @Column(name = "patient_name", length = 100)
    private String patientName;

    // EXACT LEGACY MAPPING: The old database incorrectly uses plural "bloodPressures"
    @Column(name = "bloodPressures", length = 100)
    private String bloodPressure;

    // EXACT LEGACY MAPPING: The old database typo "heatBeat"
    @Column(name = "heatBeat", length = 100)
    private String heatBeat;

    @Column(name = "oxygenSaturation", length = 100)
    private String oxygenSaturation;

    // EXACT LEGACY MAPPING: The old database typo "sugerlevel"
    @Column(name = "sugerlevel", length = 100)
    private String sugerlevel;

    @Column(name = "height", length = 100)
    private String height;

    @Column(name = "weight", length = 100)
    private String weight;

    @Column(name = "temperature", length = 100)
    private String temperature;

    @Column(name = "url", length = 1000)
    private String url;

    @Column(name = "date_string", length = 100)
    private String dateString;

    @Column(name = "hospital_id", length = 100)
    private String hospitalId;

    @Column(name = "title", length = 100)
    private String title;

    // EXACT LEGACY MAPPING: The old database typo "resparatoryRate"
    @Column(name = "resparatoryRate", length = 100)
    private String resparatoryRate;
}
