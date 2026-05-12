package com.insa.hospital.repository;

import com.insa.hospital.entity.PatientTriage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for the legacy `patient_triage` table.
 */
@Repository
public interface PatientTriageRepository extends JpaRepository<PatientTriage, Integer> {

    /** All triage records for a hospital (paginated). */
    Page<PatientTriage> findByHospitalId(String hospitalId, Pageable pageable);

    @Query("""
        SELECT p FROM PatientTriage p
        WHERE p.hospitalId IN :hospitalIds
           OR (:includeBlank = true AND COALESCE(TRIM(p.hospitalId), '') = '')
        """)
    Page<PatientTriage> findVisibleByHospitalIds(
            @Param("hospitalIds") List<String> hospitalIds,
            @Param("includeBlank") boolean includeBlank,
            Pageable pageable);

    /** All triage records for a specific patient. */
    List<PatientTriage> findByPatientAndHospitalId(String patient, String hospitalId);
    @Query("""
        SELECT p FROM PatientTriage p
        WHERE p.patient = :patient
          AND (p.hospitalId IN :hospitalIds
               OR (:includeBlank = true AND COALESCE(TRIM(p.hospitalId), '') = ''))
        ORDER BY p.id DESC
        """)
    List<PatientTriage> findVisibleByPatient(
            @Param("patient") String patient,
            @Param("hospitalIds") List<String> hospitalIds,
            @Param("includeBlank") boolean includeBlank);
    @Query("SELECT MAX(p.id) FROM PatientTriage p")
    Integer findMaxId();

    /** Most recent triage record for a patient (for vitals display on patient profile). */
    @Query(value = """
        SELECT * FROM patient_triage
        WHERE patient = :patientId
        AND hospital_id = :hospitalId
        ORDER BY CASE WHEN date ~ '^[0-9]+$' THEN CAST(date AS BIGINT) ELSE 0 END DESC
        """, nativeQuery = true)
    List<PatientTriage> findLatestByPatient(
            @Param("patientId") String patientId,
            @Param("hospitalId") String hospitalId,
            Pageable pageable);

    @Query(value = """
        SELECT * FROM patient_triage
        WHERE patient = :patientId
          AND (
              hospital_id IN (:hospitalIds)
              OR (:includeBlank = true AND COALESCE(TRIM(hospital_id), '') = '')
          )
        ORDER BY CASE WHEN date ~ '^[0-9]+$' THEN CAST(date AS BIGINT) ELSE 0 END DESC
        """, nativeQuery = true)
    List<PatientTriage> findVisibleLatestByPatient(
            @Param("patientId") String patientId,
            @Param("hospitalIds") List<String> hospitalIds,
            @Param("includeBlank") boolean includeBlank,
            Pageable pageable);

    @Query("SELECT p FROM PatientTriage p WHERE p.hospitalId = :hospitalId " +
           "AND (:name IS NULL OR :name = '' OR LOWER(p.patientName) LIKE LOWER(CONCAT('%', :name, '%')) OR p.patient = :name) " +
           "AND (:startDate IS NULL OR :startDate = '' OR p.date >= :startDate) " +
           "AND (:endDate IS NULL OR :endDate = '' OR p.date <= :endDate)")
    Page<PatientTriage> findFilteredTriage(
            @Param("hospitalId") String hospitalId,
            @Param("name") String name,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            Pageable pageable);

    @Query("""
        SELECT p FROM PatientTriage p
        WHERE (p.hospitalId IN :hospitalIds
               OR (:includeBlank = true AND COALESCE(TRIM(p.hospitalId), '') = ''))
          AND (:name IS NULL OR :name = '' OR LOWER(p.patientName) LIKE LOWER(CONCAT('%', :name, '%')) OR p.patient = :name)
          AND (:startDate IS NULL OR :startDate = '' OR p.date >= :startDate)
          AND (:endDate IS NULL OR :endDate = '' OR p.date <= :endDate)
        """)
    Page<PatientTriage> findVisibleFilteredTriage(
            @Param("hospitalIds") List<String> hospitalIds,
            @Param("includeBlank") boolean includeBlank,
            @Param("name") String name,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            Pageable pageable);
}
