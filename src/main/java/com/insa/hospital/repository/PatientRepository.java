package com.insa.hospital.repository;

import com.insa.hospital.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for the legacy `patient` table.
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, Integer> {

    /** List all patients scoped to a specific hospital (multi-tenant). */
    Page<Patient> findByHospitalId(String hospitalId, Pageable pageable);

    @Query("""
        SELECT p FROM Patient p
        WHERE p.hospitalId IN :hospitalIds
           OR (:includeBlank = true AND COALESCE(TRIM(p.hospitalId), '') = '')
        """)
    Page<Patient> findVisibleByHospitalIds(
            @Param("hospitalIds") List<String> hospitalIds,
            @Param("includeBlank") boolean includeBlank,
            Pageable pageable);

    @Query("""
        SELECT p FROM Patient p
        WHERE p.hospitalId IN :hospitalIds
           OR (:includeBlank = true AND COALESCE(TRIM(p.hospitalId), '') = '')
        """)
    List<Patient> findVisibleByHospitalIds(
            @Param("hospitalIds") List<String> hospitalIds,
            @Param("includeBlank") boolean includeBlank);

    /**
     * Search patients by name OR phone — multi-tenant scoped.
     * Uses LIKE for partial matching (mirrors legacy PHP search behaviour).
     */
    @Query("""
        SELECT p FROM Patient p
        WHERE p.hospitalId = :hospitalId
        AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
             OR p.phone LIKE CONCAT('%', :query, '%')
             OR p.patientId LIKE CONCAT('%', :query, '%'))
        """)
    Page<Patient> searchByNameOrPhoneOrPatientId(
            @Param("hospitalId") String hospitalId,
            @Param("query") String query,
            Pageable pageable);

    @Query("""
        SELECT p FROM Patient p
        WHERE (p.hospitalId IN :hospitalIds
           OR (:includeBlank = true AND COALESCE(TRIM(p.hospitalId), '') = ''))
          AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
             OR p.phone LIKE CONCAT('%', :query, '%')
             OR p.patientId LIKE CONCAT('%', :query, '%'))
        """)
    Page<Patient> searchVisibleByNameOrPhoneOrPatientId(
            @Param("hospitalIds") List<String> hospitalIds,
            @Param("includeBlank") boolean includeBlank,
            @Param("query") String query,
            Pageable pageable);

    /** Check if a legacy patient_id (6-digit) is already taken. */
    boolean existsByPatientId(String patientId);

    /** Find a single patient by their 6-digit legacy patient_id, scoped to hospital. */
    Optional<Patient> findByPatientIdAndHospitalId(String patientId, String hospitalId);

    List<Patient> findByHospitalIdAndPatientIdIn(String hospitalId, List<String> patientIds);

    /** Find a patient by their associated ion_user_id (for authentication linkage). */
    java.util.Optional<Patient> findByIonUserId(String ionUserId);

    // ─── Dashboard Aggregates ──────────────────────────────────────────────────

    /** Total patient count for hospital. */
    long countByHospitalId(String hospitalId);

    /**
     * Count patients registered today.
     * registration_time is a Unix epoch varchar — use CAST(AS UNSIGNED) for numeric compare.
     */
    @Query(value = """
        SELECT COUNT(*) FROM patient
        WHERE hospital_id = :hospitalId
        AND CASE WHEN registration_time ~ '^[0-9]+$' THEN CAST(registration_time AS BIGINT) ELSE NULL END BETWEEN :startEpoch AND :endEpoch
        """, nativeQuery = true)
    long countTodayRegistrations(
            @Param("hospitalId") String hospitalId,
            @Param("startEpoch") long startEpoch,
            @Param("endEpoch") long endEpoch);

    /** 5 most recently registered patients (latest registration_time first). */
    @Query(value = """
        SELECT * FROM patient
        WHERE hospital_id = :hospitalId
        ORDER BY CASE WHEN registration_time ~ '^[0-9]+$' THEN CAST(registration_time AS BIGINT) ELSE 0 END DESC
        LIMIT 5
        """, nativeQuery = true)
    List<Patient> findTop5RecentByHospitalId(@Param("hospitalId") String hospitalId);

    /** All patients registered today. */
    @Query(value = """
        SELECT * FROM patient
        WHERE hospital_id = :hospitalId
        AND CASE WHEN registration_time ~ '^[0-9]+$' THEN CAST(registration_time AS BIGINT) ELSE NULL END BETWEEN :startEpoch AND :endEpoch
        ORDER BY CASE WHEN registration_time ~ '^[0-9]+$' THEN CAST(registration_time AS BIGINT) ELSE 0 END DESC
        """, nativeQuery = true)
    List<Patient> findTodayRegistrations(
            @Param("hospitalId") String hospitalId,
            @Param("startEpoch") long startEpoch,
            @Param("endEpoch") long endEpoch);

}
