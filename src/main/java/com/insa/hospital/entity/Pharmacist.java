package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity mapping the legacy `pharmacist` table.
 *
 * Column mapping from nhospital1 SQL dump (lines 1616–1627).
 * ALL 10 columns mapped 1:1 — DO NOT add, rename, or remove columns.
 *
 * x, y are legacy spare columns — preserved per agent.md §4.5.
 * ion_user_id → references users.id (the login account for this pharmacist).
 */
@Entity
@Table(name = "pharmacist")
@Getter
@Setter
@NoArgsConstructor
public class Pharmacist {

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

    /** Legacy spare column x. */
    @Column(name = "x", length = 100)
    private String x;

    /** Legacy spare column y. */
    @Column(name = "y", length = 100)
    private String y;

    /** References users.id for the pharmacist's login account. */
    @Column(name = "ion_user_id", length = 100)
    private String ionUserId;

    /** Multi-tenant hospital identifier. */
    @Column(name = "hospital_id", length = 100)
    private String hospitalId;
}
