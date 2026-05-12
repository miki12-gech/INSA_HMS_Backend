package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity mapping the legacy `nurse` table.
 *
 * Column mapping from nhospital1 SQL dump (lines 1114–1126).
 * ALL 11 columns mapped 1:1.
 *
 * x, y, z are legacy spare columns — preserved per agent.md §4.5.
 * ion_user_id → references users.id (the login account for this nurse).
 */
@Entity
@Table(name = "nurse")
@Getter
@Setter
@NoArgsConstructor
public class Nurse {

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

    /** Legacy spare column z. */
    @Column(name = "z", length = 100)
    private String z;

    /** References users.id for the nurse's login account. */
    @Column(name = "ion_user_id", length = 100)
    private String ionUserId;

    /** Multi-tenant hospital identifier. */
    @Column(name = "hospital_id", length = 100)
    private String hospitalId;
}
