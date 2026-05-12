package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity for the legacy `groups` table (ion_auth roles).
 *
 * Groups defined in the legacy system:
 *  id=1  → superadmin
 *  id=2  → members (General User)
 *  id=3  → Accountant
 *  id=4  → Doctor
 *  id=5  → Patient
 *  id=6  → Nurse
 *  id=7  → Pharmacist
 *  id=8  → Laboratorist
 *  id=10 → Receptionist
 *  id=11 → admin (Hospital Administrator)
 */
@Entity
@Table(name = "groups")
@Getter
@Setter
@NoArgsConstructor
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /** Role name — e.g. "Doctor", "Nurse", "superadmin", "admin". */
    @Column(name = "name", nullable = false, length = 20)
    private String name;

    @Column(name = "description", nullable = false, length = 100)
    private String description;
}
