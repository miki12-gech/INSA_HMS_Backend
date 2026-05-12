package com.insa.hospital.repository;

import com.insa.hospital.entity.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for the legacy `users_groups` join table (ion_auth).
 */
@Repository
public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {

    Optional<UserGroup> findByUserId(Long userId);

    /** Gets the group name for a given user by joining through the groups table. */
    @Query("SELECT g.name FROM Group g JOIN UserGroup ug ON g.id = ug.groupId WHERE ug.userId = :userId")
    Optional<String> findGroupNameByUserId(@Param("userId") Long userId);
}
