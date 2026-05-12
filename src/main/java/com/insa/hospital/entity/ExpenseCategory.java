package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity mapping the legacy `expense_category` table.
 *
 * Column mapping from nhospital1 SQL dump (lines 1546–1553).
 * ALL 6 columns mapped 1:1 — DO NOT add, rename, or remove columns.
 *
 * x, y are legacy spare columns — preserved per agent.md §4.5.
 */
@Entity
@Table(name = "expense_category")
@Getter
@Setter
@NoArgsConstructor
public class ExpenseCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "description", length = 100)
    private String description;

    /** Legacy spare column x — mapped as-is, do NOT rename. */
    @Column(name = "x", length = 100)
    private String x;

    /** Legacy spare column y — mapped as-is, do NOT rename. */
    @Column(name = "y", length = 100)
    private String y;

    /** Multi-tenant hospital identifier. */
    @Column(name = "hospital_id", length = 100)
    private String hospitalId;
}
