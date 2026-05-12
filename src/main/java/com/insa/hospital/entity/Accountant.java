package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity mapping the legacy `accountant` table.
 *
 * Column mapping from nhospital1 SQL dump (lines 30–40).
 * ALL 9 columns mapped 1:1 — DO NOT add, rename, or remove columns.
 *
 * x is a legacy spare column — preserved per agent.md §4.5.
 * ion_user_id → references users.id (the login account for this accountant).
 */
@Entity
@Table(name = "accountant")
@Getter
@Setter
@NoArgsConstructor
public class Accountant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "img_url", length = 200)
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

    /** References users.id for the accountant's login account. */
    @Column(name = "ion_user_id", length = 100)
    private String ionUserId;

    /** Multi-tenant hospital identifier. */
    @Column(name = "hospital_id", length = 100)
    private String hospitalId;
}
