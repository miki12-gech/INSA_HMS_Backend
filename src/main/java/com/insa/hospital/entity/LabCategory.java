package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "lab_category")
@Getter
@Setter
public class LabCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String category;
    private String description;

    @Column(name = "reference_value", length = 1000)
    private String referenceValue;

    @Column(name = "hospital_id")
    private String hospitalId;
}
