package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "bed")
@Getter
@Setter
@NoArgsConstructor
public class Bed {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "number", length = 100)
    private String number;

    @Column(name = "description", length = 100)
    private String description;

    @Column(name = "last_a_time", length = 100)
    private String lastATime;

    @Column(name = "last_d_time", length = 100)
    private String lastDTime;

    @Column(name = "status", length = 100)
    private String status;

    @Column(name = "bed_id", length = 100)
    private String bedId;

    @Column(name = "hospital_id", length = 100)
    private String hospitalId;
}
