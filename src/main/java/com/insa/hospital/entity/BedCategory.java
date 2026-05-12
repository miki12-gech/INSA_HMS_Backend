package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "bed_category")
@Getter
@Setter
@NoArgsConstructor
public class BedCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "description", length = 100)
    private String description;

    @Column(name = "hospital_id", length = 100)
    private String hospitalId;
}
