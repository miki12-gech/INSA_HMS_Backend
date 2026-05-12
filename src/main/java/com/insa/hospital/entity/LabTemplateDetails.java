package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "lab_template_details")
@Getter
@Setter
public class LabTemplateDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lab_id")
    private String labId;

    @Column(name = "template_id")
    private String templateId;

    @Column(name = "hospital_id")
    private String hospitalId;
}
