package com.insa.hospital.repository;

import com.insa.hospital.entity.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for the legacy `appointment` table.
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {

    /** All appointments for a hospital (paginated). */
    Page<Appointment> findByHospitalId(String hospitalId, Pageable pageable);

    /** All appointments for a hospital, unpaged (for calendar). */
    List<Appointment> findByHospitalIdOrderByIdAsc(String hospitalId);

    /** All appointments for a doctor, unpaged. */
    List<Appointment> findByDoctorAndHospitalId(String doctor, String hospitalId);

    /** All appointments for a patient, unpaged. */
    List<Appointment> findByPatientAndHospitalId(String patient, String hospitalId);

    /** Appointments for a specific doctor on a specific date (Unix epoch string). */
    List<Appointment> findByDoctorAndDateAndHospitalId(String doctor, String date, String hospitalId);

    /** Appointments for a specific doctor (paginated). */
    Page<Appointment> findByDoctorAndHospitalId(String doctor, String hospitalId, Pageable pageable);

    /** Appointments for a specific patient (paginated). */
    Page<Appointment> findByPatientAndHospitalId(String patient, String hospitalId, Pageable pageable);

    /** Appointments by status (e.g. 'Pending Confirmation'). */
    Page<Appointment> findByStatusAndHospitalId(String status, String hospitalId, Pageable pageable);

    /** Appointments by date range and hospital (for day-view/week-view scheduler). */
    @Query(value = """
        SELECT * FROM appointment
        WHERE hospital_id = :hospitalId
        AND CASE WHEN date ~ '^[0-9]+$' THEN CAST(date AS BIGINT) ELSE NULL END BETWEEN :startEpoch AND :endEpoch
        ORDER BY CASE WHEN date ~ '^[0-9]+$' THEN CAST(date AS BIGINT) ELSE 0 END ASC
        """, nativeQuery = true)
    List<Appointment> findByDateRangeAndHospitalId(
            @Param("hospitalId") String hospitalId,
            @Param("startEpoch") long startEpoch,
            @Param("endEpoch") long endEpoch);

    /** Count appointments for a doctor on a given date (for slot availability check). */
    long countByDoctorAndDateAndHospitalId(String doctor, String date, String hospitalId);

    /** Search appointments by patient name or doctor name via join-free JPQL. */
    @Query("""
        SELECT a FROM Appointment a
        WHERE a.hospitalId = :hospitalId
        AND (a.patient LIKE CONCAT('%', :query, '%')
             OR a.doctor LIKE CONCAT('%', :query, '%')
             OR a.status LIKE CONCAT('%', :query, '%'))
        """)
    Page<Appointment> searchAppointments(
            @Param("hospitalId") String hospitalId,
            @Param("query") String query,
            Pageable pageable);

    // ─── Dashboard Aggregates ──────────────────────────────────────────────────

    /** Total appointments for hospital. */
    long countByHospitalId(String hospitalId);

    /** Count appointments within today's epoch range. */
    @Query(value = """
        SELECT COUNT(*) FROM appointment
        WHERE hospital_id = :hospitalId
        AND CASE WHEN date ~ '^[0-9]+$' THEN CAST(date AS BIGINT) ELSE NULL END BETWEEN :startEpoch AND :endEpoch
        """, nativeQuery = true)
    long countTodayAppointments(
            @Param("hospitalId") String hospitalId,
            @Param("startEpoch") long startEpoch,
            @Param("endEpoch") long endEpoch);

    /** Count pending-confirmation appointments. */
    long countByStatusAndHospitalId(String status, String hospitalId);

    /**
     * Find appointments submitted via the legacy request workflow.
     * These are appointments where the `request` column is populated
     * (non-null and non-empty) — i.e., external/patient-initiated requests.
     */
    @Query("""
        SELECT a FROM Appointment a
        WHERE a.hospitalId = :hospitalId
        AND a.request IS NOT NULL
        AND a.request <> ''
        """)
    Page<Appointment> findRequestedAppointments(
            @Param("hospitalId") String hospitalId,
            Pageable pageable);
}
