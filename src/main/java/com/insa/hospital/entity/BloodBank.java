package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity mapping the legacy `bankb` (Blood Bank) table.
 *
 * Column mapping from nhospital1 SQL dump (lines 130–135).
 * ALL 4 columns mapped 1:1 — DO NOT add, rename, or remove columns.
 *
 * Key legacy observations (from sample data lines 141–213):
 *  - group  : blood group e.g. "A+", "A-", "B+", "O-" etc.
 *  - status : stock status string e.g. "0 Bags", "10 Bags"
 *
 * Note: `group` is a reserved SQL word — the column name is escaped in the annotation.
 */
@Entity
@Table(name = "bankb")
@Getter
@Setter
@NoArgsConstructor
public class BloodBank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /**
     * Blood group — e.g. "A+", "A-", "B+", "O-".
     * Legacy column is named `group` (a reserved SQL word).
     */
    @Column(name = "`group`", length = 100)
    private String group;

    /** Stock status — e.g. "0 Bags", "10 Bags". */
    @Column(name = "status", length = 100)
    private String status;

    /** Multi-tenant hospital identifier. */
    @Column(name = "hospital_id", length = 100)
    private String hospitalId;
}
