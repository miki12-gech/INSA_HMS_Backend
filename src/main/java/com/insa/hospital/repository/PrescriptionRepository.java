package com.insa.hospital.repository;

import com.insa.hospital.entity.Prescription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Integer> {

    Page<Prescription> findByHospitalId(String hospitalId, Pageable pageable);

    @Query("""
        SELECT p FROM Prescription p
        WHERE p.hospitalId IN :hospitalIds
           OR (:includeBlank = true AND COALESCE(TRIM(p.hospitalId), '') = '')
        ORDER BY p.id DESC
        """)
    List<Prescription> findVisibleByHospitalIdsOrderByIdDesc(
            @Param("hospitalIds") List<String> hospitalIds,
            @Param("includeBlank") boolean includeBlank);

    List<Prescription> findByPatientAndHospitalId(String patient, String hospitalId);

    @Query("""
        SELECT p FROM Prescription p
        WHERE p.patient = :patient
          AND (p.hospitalId IN :hospitalIds
               OR (:includeBlank = true AND COALESCE(TRIM(p.hospitalId), '') = ''))
        ORDER BY p.id DESC
        """)
    List<Prescription> findVisibleByPatient(
            @Param("patient") String patient,
            @Param("hospitalIds") List<String> hospitalIds,
            @Param("includeBlank") boolean includeBlank);

    Page<Prescription> findByDoctorAndHospitalId(String doctor, String hospitalId, Pageable pageable);

    @Query("""
        SELECT p FROM Prescription p
        WHERE p.doctor = :doctor
          AND (p.hospitalId IN :hospitalIds
               OR (:includeBlank = true AND COALESCE(TRIM(p.hospitalId), '') = ''))
        """)
    Page<Prescription> findVisibleByDoctor(
            @Param("doctor") String doctor,
            @Param("hospitalIds") List<String> hospitalIds,
            @Param("includeBlank") boolean includeBlank,
            Pageable pageable);

    Page<Prescription> findByPatientAndHospitalId(String patient, String hospitalId, Pageable pageable);

    /** All prescriptions for hospital, ordered by ID desc (newest first). */
    List<Prescription> findByHospitalIdOrderByIdDesc(String hospitalId);

    /** Pending prescriptions — exclude specific states (e.g. DISPENSED, COMPLETED). Includes NULL states. */
    @Query("SELECT p FROM Prescription p WHERE p.hospitalId = :hospitalId AND (p.state IS NULL OR p.state NOT IN :excludedStates) ORDER BY p.id DESC")
    List<Prescription> findByHospitalIdAndStateNotIn(
            @Param("hospitalId") String hospitalId,
            @Param("excludedStates") List<String> excludedStates);

    @Query("""
        SELECT p FROM Prescription p
        WHERE (p.hospitalId IN :hospitalIds
               OR (:includeBlank = true AND COALESCE(TRIM(p.hospitalId), '') = ''))
          AND (p.state IS NULL OR p.state NOT IN :excludedStates)
        ORDER BY p.id DESC
        """)
    List<Prescription> findVisiblePendingByHospitalIds(
            @Param("hospitalIds") List<String> hospitalIds,
            @Param("includeBlank") boolean includeBlank,
            @Param("excludedStates") List<String> excludedStates);

    /** All prescriptions with a specific state. */
    List<Prescription> findByHospitalIdAndState(String hospitalId, String state);
}
