package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity mapping the legacy `donor` table.
 *
 * Column mapping from nhospital1 SQL dump (lines 466–477).
 * ALL 10 columns mapped 1:1 — DO NOT add, rename, or remove columns.
 *
 * Key legacy observations:
 *  - group  : blood group e.g. "A+", "B-", "O+" (use backtick in SQL — mapped as `group`)
 *  - ldd    : last donation date (stored as string)
 *  - sex    : "Male" | "Female"
 *  - age    : stored as varchar (e.g. "25")
 *  - add_date : registration date string
 */
@Entity
@Table(name = "donor")
@Getter
@Setter
@NoArgsConstructor
public class Donor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", length = 100)
    private String name;

    /**
     * Blood group — e.g. "A+", "B-", "O+".
     * Legacy column is named `group` (a reserved word in SQL).
     * JPA column annotation escapes it properly.
     */
    @Column(name = "`group`", length = 10)
    private String group;

    /** Donor age stored as varchar. */
    @Column(name = "age", length = 10)
    private String age;

    /** "Male" | "Female" */
    @Column(name = "sex", length = 10)
    private String sex;

    /** Last donation date — stored as string. */
    @Column(name = "ldd", length = 100)
    private String ldd;

    @Column(name = "phone", length = 100)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    /** Registration date string. */
    @Column(name = "add_date", length = 100)
    private String addDate;

    /** Multi-tenant hospital identifier. */
    @Column(name = "hospital_id", length = 100)
    private String hospitalId;
}
