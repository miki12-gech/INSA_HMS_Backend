package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity mapping the legacy `hospital` table.
 *
 * Column mapping from nhospital1 SQL dump (lines 626–638).
 * ALL 11 columns mapped 1:1.
 *
 * IMPORTANT: In the legacy system, this table IS the system settings.
 * There is NO separate `settings` table. Each row represents one hospital
 * tenant. This entity is used by SettingsService to fetch/update
 * the hospital's public info (name, address, phone) and module list.
 *
 * Key columns:
 *  - module  : comma-separated list of enabled modules for this hospital
 *              e.g. "accountant,appointment,lab,bed,doctor,finance,..."
 *  - package : SaaS package tier (not used in migration)
 *  - p_limit : patient roster limit
 *  - d_limit : doctor roster limit
 *  - ion_user_id: references users.id of the admin for this hospital
 *
 * SECURITY: The `password` column must NEVER be sent to the frontend.
 * The SettingsResponseDto explicitly omits it.
 */
@Entity
@Table(name = "hospital")
@Getter
@Setter
@NoArgsConstructor
public class Hospital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", length = 500)
    private String name;

    @Column(name = "email", length = 500)
    private String email;

    /** NEVER expose this field in response DTOs. */
    @Column(name = "password", length = 500)
    private String password;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "phone", length = 500)
    private String phone;

    @Column(name = "package", length = 100)
    private String hospitalPackage;

    /** Maximum patient count limit. */
    @Column(name = "p_limit", length = 100)
    private String pLimit;

    /** Maximum doctor count limit. */
    @Column(name = "d_limit", length = 100)
    private String dLimit;

    /**
     * Comma-separated active module list.
     * e.g. "accountant,appointment,lab,bed,department,doctor,finance,..."
     * Used by frontend to show/hide feature navigation.
     */
    @Column(name = "module", length = 1000)
    private String module;

    /** references users.id of the super-admin for this hospital tenant. */
    @Column(name = "ion_user_id", length = 100)
    private String ionUserId;
}
