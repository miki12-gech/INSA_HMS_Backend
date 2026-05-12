package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity mapping the legacy `doctor` table.
 *
 * Column mapping from nhospital1 SQL dump (lines 433–446).
 * ALL 12 columns mapped 1:1 — DO NOT add, rename, or remove columns.
 *
 * Key observations from actual data rows:
 *  - ion_user_id : references users.id (the doctor's ion_auth login account)
 *  - hospital_id : multi-tenant identifier (varchar, not FK constraint)
 *  - department  : specialty name (e.g. "Cardiology", "General GP")
 *  - profile     : short text description of specialization
 *  - x, y        : legacy spare columns, purpose unclear from schema — mapped as-is
 */
@Entity
@Table(name = "doctor")
@Getter
@Setter
@NoArgsConstructor
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "img_url", length = 100)
    private String imgUrl;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "address", length = 100)
    private String address;

    @Column(name = "phone", length = 100)
    private String phone;

    /** Medical department / specialty. e.g. "Cardiology", "General surgery". */
    @Column(name = "department", length = 100)
    private String department;

    /** Short profile/bio text. */
    @Column(name = "profile", length = 100)
    private String profile;

    /** Legacy spare column x — mapped as-is, do NOT rename. */
    @Column(name = "x", length = 100)
    private String x;

    /** Legacy spare column y — mapped as-is, do NOT rename. */
    @Column(name = "y", length = 10)
    private String y;

    /**
     * References users.id — the ion_auth account linked to this doctor.
     * Used to resolve the doctor's login credentials from the users table.
     */
    @Column(name = "ion_user_id", length = 100)
    private String ionUserId;

    /** Multi-tenant hospital identifier. */
    @Column(name = "hospital_id", length = 100)
    private String hospitalId;
}
