package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity mapping the legacy `department` table.
 *
 * Column mapping from nhospital1 SQL dump (lines 275–282).
 * ALL 6 columns mapped 1:1.
 *
 * Used for department listing/management in admin module.
 * x, y are legacy spare columns preserved as-is per agent.md §4.5.
 */
@Entity
@Table(name = "department")
@Getter
@Setter
@NoArgsConstructor
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", length = 100)
    private String name;

    /** Department description — may contain raw HTML. */
    @Column(name = "description", length = 1000)
    private String description;

    /** Legacy spare column. */
    @Column(name = "x", length = 10)
    private String x;

    /** Legacy spare column. */
    @Column(name = "y", length = 10)
    private String y;

    @Column(name = "hospital_id", length = 100)
    private String hospitalId;
}
