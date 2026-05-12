package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity mapping the legacy ion_auth `users` table.
 *
 * COLUMN MAPPING RULES (from nhospital1 SQL dump, lines 2241-2260):
 * - All column names mapped 1:1 using @Column(name="...")
 * - id is UNSIGNED int(11) — mapped as Long for safety
 * - password contains $2y$ BCrypt hashes from PHP's password_hash()
 * - BCryptPasswordEncoder is fully compatible with PHP's $2y$ prefix
 * - created_on and last_login are Unix timestamp integers (mapped as Long)
 * - active is tinyint(1) — mapped as Integer (0=inactive, 1=active)
 * - hospital_ion_id is the reference back to the hospital table's ion_user_id
 *
 * DO NOT add, rename, or remove any columns.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "ip_address", nullable = false, length = 15)
    private String ipAddress;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    /** BCrypt hash (PHP $2y$, fully compatible with Spring BCryptPasswordEncoder). */
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    /** Legacy salt field — ion_auth stored this separately but password is self-contained BCrypt. */
    @Column(name = "salt", length = 255)
    private String salt;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "activation_code", length = 40)
    private String activationCode;

    @Column(name = "forgotten_password_code", length = 40)
    private String forgottenPasswordCode;

    /** Unix timestamp when the forgotten-password token expires. */
    @Column(name = "forgotten_password_time")
    private Long forgottenPasswordTime;

    @Column(name = "remember_code", length = 40)
    private String rememberCode;

    /** Unix timestamp when the user account was created. */
    @Column(name = "created_on", nullable = false)
    private Long createdOn;

    /** Unix timestamp of the last login. Nullable for new accounts. */
    @Column(name = "last_login")
    private Long lastLogin;

    /**
     * Account active flag: 1=active, 0=inactive.
     * Mapped as Integer because tinyint(1) UNSIGNED in MySQL.
     */
    @Column(name = "active")
    private Integer active;

    @Column(name = "first_name", length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    @Column(name = "company", length = 100)
    private String company;

    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * Links this user to their hospital in the multi-tenant setup.
     * References hospital.ion_user_id (admin user of that hospital).
     */
    @Column(name = "hospital_ion_id", length = 100)
    private String hospitalIonId;
}
