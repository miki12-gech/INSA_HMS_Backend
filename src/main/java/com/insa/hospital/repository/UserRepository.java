package com.insa.hospital.repository;

import com.insa.hospital.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for the legacy `users` table (ion_auth).
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** Used by UserDetailsService to load a user for login via email. */
    Optional<User> findByEmail(String email);

    /** Case-insensitive email lookup for login and dev bootstrap users. */
    Optional<User> findByEmailIgnoreCase(String email);

    /** Case-insensitive email uniqueness check. */
    boolean existsByEmailIgnoreCase(String email);

    /** Username-based lookup (legacy fallback). */
    Optional<User> findByUsername(String username);

    /** Case-insensitive username lookup for login fallback. */
    Optional<User> findByUsernameIgnoreCase(String username);

    /** Check email uniqueness before creating new users. */
    boolean existsByEmail(String email);

    /**
     * Finds a user along with their group name with a single JOIN query.
     * Returns Object[] { User, groupName:String }.
     */
    @Query("""
        SELECT u, g.name
        FROM User u
        JOIN UserGroup ug ON ug.userId = u.id
        JOIN Group g ON g.id = ug.groupId
        WHERE u.email = :email
        """)
    Optional<Object[]> findUserWithGroupByEmail(@Param("email") String email);

    /**
     * Finds a user along with their group name with a single JOIN query by ID.
     * Returns Object[] { User, groupName:String }.
     */
    @Query("""
        SELECT u, g.name
        FROM User u
        JOIN UserGroup ug ON ug.userId = u.id
        JOIN Group g ON g.id = ug.groupId
        WHERE u.id = :id
        """)
    Optional<Object[]> findUserWithGroupById(@Param("id") Long id);

    /**
     * Finds Employee users by active state, optionally scoped to a hospital.
     * Used for the manual approval queue.
     */
    @Query("""
        SELECT u
        FROM User u
        JOIN UserGroup ug ON ug.userId = u.id
        JOIN Group g ON g.id = ug.groupId
        WHERE LOWER(g.name) = LOWER(:groupName)
          AND u.active = :active
          AND (:hospitalIonId IS NULL OR u.hospitalIonId = :hospitalIonId)
        ORDER BY u.createdOn DESC
        """)
    List<User> findByGroupNameAndActive(
            @Param("groupName") String groupName,
            @Param("active") Integer active,
            @Param("hospitalIonId") String hospitalIonId);

    /**
     * Finds a single user by id + auth group name.
     */
    @Query("""
        SELECT u
        FROM User u
        JOIN UserGroup ug ON ug.userId = u.id
        JOIN Group g ON g.id = ug.groupId
        WHERE u.id = :id
          AND LOWER(g.name) = LOWER(:groupName)
        """)
    Optional<User> findByIdAndGroupName(
            @Param("id") Long id,
            @Param("groupName") String groupName);

    /**
     * Checks whether an INSA ID card number is already attached to an Employee account.
     * For this legacy schema POC, the value is stored in users.company.
     */
    @Query("""
        SELECT COUNT(u) > 0
        FROM User u
        JOIN UserGroup ug ON ug.userId = u.id
        JOIN Group g ON g.id = ug.groupId
        WHERE LOWER(g.name) = LOWER(:groupName)
          AND LOWER(COALESCE(u.company, '')) = LOWER(:company)
        """)
    boolean existsByCompanyAndGroupName(
            @Param("company") String company,
            @Param("groupName") String groupName);
}
