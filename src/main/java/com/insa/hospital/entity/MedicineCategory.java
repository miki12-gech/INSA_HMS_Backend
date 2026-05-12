package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity mapping the legacy `medicine_category` table.
 * Columns: id, category, description, hospital_id (lines 945–950)
 * Used as the primary medicine category (dosage form/type).
 */
@Entity
@Table(name = "medicine_category")
@Getter
@Setter
@NoArgsConstructor
public class MedicineCategory {

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
