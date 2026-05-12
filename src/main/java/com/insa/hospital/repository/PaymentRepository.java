package com.insa.hospital.repository;

import com.insa.hospital.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    Page<Payment> findByHospitalId(String hospitalId, Pageable pageable);

    /** All invoices for a specific patient (for patient profile billing tab). */
    List<Payment> findByPatientAndHospitalId(String patient, String hospitalId);

    Page<Payment> findByStatusAndHospitalId(String status, String hospitalId, Pageable pageable);

    Page<Payment> findByDoctorAndHospitalId(String doctor, String hospitalId, Pageable pageable);

    // ─── Dashboard Aggregates ──────────────────────────────────────────────────

    /**
     * Sum gross_total for payments created today.
     * date is Unix epoch varchar. gross_total is varchar — CAST both for math.
     * Returns null if no payments today — handle in service with Optional/default.
     */
    @Query(value = """
        SELECT COALESCE(SUM(CAST(gross_total AS DECIMAL(15,2))), 0)
        FROM payment
        WHERE hospital_id = :hospitalId
        AND CASE WHEN date ~ '^[0-9]+$' THEN CAST(date AS BIGINT) ELSE NULL END BETWEEN :startEpoch AND :endEpoch
        """, nativeQuery = true)
    Double sumTodayRevenue(
            @Param("hospitalId") String hospitalId,
            @Param("startEpoch") long startEpoch,
            @Param("endEpoch") long endEpoch);

    /** Count unpaid invoices for hospital. */
    long countByStatusAndHospitalId(String status, String hospitalId);

    /** 5 most recent payments. */
    @Query(value = """
        SELECT * FROM payment
        WHERE hospital_id = :hospitalId
        ORDER BY CASE WHEN date ~ '^[0-9]+$' THEN CAST(date AS BIGINT) ELSE 0 END DESC
        LIMIT 5
        """, nativeQuery = true)
    java.util.List<Payment> findTop5RecentByHospitalId(@Param("hospitalId") String hospitalId);
}
