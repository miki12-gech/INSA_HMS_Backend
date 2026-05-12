package com.insa.hospital.repository;

import com.insa.hospital.entity.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for the legacy `doctor` table.
 */
@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Integer> {

    /** List all doctors for a hospital (multi-tenant). */
    Page<Doctor> findByHospitalId(String hospitalId, Pageable pageable);

    /** All doctors for a hospital, unpaged — for dropdown lists. */
    List<Doctor> findByHospitalId(String hospitalId);

    /** Find by ion_user_id (links doctor profile to login account). */
    Optional<Doctor> findByIonUserId(String ionUserId);

    /** Find by ion_user_id and hospitalId (safe multi-tenant lookup). */
    Optional<Doctor> findByIonUserIdAndHospitalId(String ionUserId, String hospitalId);

    /** Batch resolve doctor profiles from login user ids for one hospital. */
    List<Doctor> findByIonUserIdInAndHospitalId(List<String> ionUserIds, String hospitalId);

    /** Batch resolve doctor profiles from login user ids without hospital scoping. */
    List<Doctor> findByIonUserIdIn(List<String> ionUserIds);

    /** Search by name or department. */
    @Query("""
        SELECT d FROM Doctor d
        WHERE d.hospitalId = :hospitalId
        AND (LOWER(d.name) LIKE LOWER(CONCAT('%', :query, '%'))
             OR LOWER(d.department) LIKE LOWER(CONCAT('%', :query, '%')))
        """)
    Page<Doctor> searchByNameOrDepartment(
            @Param("hospitalId") String hospitalId,
            @Param("query") String query,
            Pageable pageable);

    /**
     * Count doctors for a hospital — used by the d_limit roster check in DoctorService.
     * AUDIT FIX — Alert 6.
     */
    long countByHospitalId(String hospitalId);
}
