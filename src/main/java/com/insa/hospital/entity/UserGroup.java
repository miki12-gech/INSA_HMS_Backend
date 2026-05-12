package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity for the legacy `users_groups` join table.
 *
 * Maps the many-to-many relationship between users and groups in ion_auth.
 * In practice, each user has exactly one group (one role).
 */
@Entity
@Table(name = "users_groups")
@Getter
@Setter
@NoArgsConstructor
public class UserGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "group_id", nullable = false)
    private Integer groupId;
}
