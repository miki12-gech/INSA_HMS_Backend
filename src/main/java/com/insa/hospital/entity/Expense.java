package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity mapping the legacy `expense` table.
 *
 * Column mapping from nhospital1 SQL dump (lines 1530–1538).
 * ALL 7 columns mapped 1:1 — DO NOT add, rename, or remove columns.
 *
 * Key legacy observations:
 *  - category  : name of the expense category (string, not FK)
 *  - date      : Unix epoch string or date string
 *  - amount    : stored as varchar (e.g. "500.00")
 *  - user      : references users.id (stored as String)
 */
@Entity
@Table(name = "expense")
@Getter
@Setter
@NoArgsConstructor
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /** Name of expense category (stored as string, not FK). */
    @Column(name = "category", length = 100)
    private String category;

    /** Date of expense — Unix epoch or date string. */
    @Column(name = "date", length = 100)
    private String date;

    /** Free text note/description for the expense. */
    @Column(name = "note", length = 1000)
    private String note;

    /** Amount as string (e.g. "500.00"). */
    @Column(name = "amount", length = 100)
    private String amount;

    /** References users.id of the staff who recorded this expense. */
    @Column(name = "\"user\"", length = 100)
    private String user;

    /** Multi-tenant hospital identifier. */
    @Column(name = "hospital_id", length = 100)
    private String hospitalId;
}
