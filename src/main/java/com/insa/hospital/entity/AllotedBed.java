package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "alloted_bed")
@Getter
@Setter
@NoArgsConstructor
public class AllotedBed {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "number", length = 100)
    private String number;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "patient", length = 100)
    private String patient;

    @Column(name = "a_time", length = 100)
    private String aTime;

    @Column(name = "d_time", length = 100)
    private String dTime;

    @Column(name = "status", length = 100)
    private String status;

    @Column(name = "x", length = 100)
    private String x;

    @Column(name = "bed_id", length = 100)
    private String bedId;

    @Column(name = "hospital_id", length = 100)
    private String hospitalId;
}
