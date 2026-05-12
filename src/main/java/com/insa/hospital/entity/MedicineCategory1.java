package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity mapping the legacy `medicine_category1` table.
 * Used as the secondary medicine category (e.g. sub-category).
 */
@Entity
@Table(name = "medicine_category1")
@Getter
@Setter
@NoArgsConstructor
public class MedicineCategory1 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "description", length = 100)
    private String description;

    @Column(name = "hospital_id", length = 100)
    private String hospitalId;
}
