package com.insa.hospital.repository;

import com.insa.hospital.entity.ReferralRequest;
import com.insa.hospital.entity.ReferralRequest.ReferralStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for the `referral_requests` table.
 *
 * All finders are scoped to hospitalId for multi-tenancy.
 */
@Repository
public interface ReferralRequestRepository extends JpaRepository<ReferralRequest, Long> {

    // ─── By Patient ──────────────────────────────────────────────────────────

    /** All referrals for a specific patient within a hospital. */
    List<ReferralRequest> findByPatientIdAndHospitalIdOrderByIdDesc(
            String patientId, String hospitalId);

    // ─── By Status ───────────────────────────────────────────────────────────

    /** All referrals with a given status within a hospital (e.g. PENDING for admin queue). */
    List<ReferralRequest> findByStatusAndHospitalIdOrderByIdDesc(
            ReferralStatus status, String hospitalId);

    // ─── By Requester ────────────────────────────────────────────────────────

    /** All referrals submitted by a specific user (Employee self-service "My Requests"). */
    List<ReferralRequest> findByRequestedByUserIdAndHospitalIdOrderByIdDesc(
            String requestedByUserId, String hospitalId);

    // ─── By ID + Hospital (multi-tenant safe lookup) ─────────────────────────

    /** Find a single referral by its PK, scoped to the caller's hospital. */
    Optional<ReferralRequest> findByIdAndHospitalId(Long id, String hospitalId);

    // ─── Combined Filters ────────────────────────────────────────────────────

    /** Referrals for a patient filtered by status (e.g. show only APPROVED for a patient). */
    @Query("""
        SELECT r FROM ReferralRequest r
        WHERE r.patientId = :patientId
        AND r.status = :status
        AND r.hospitalId = :hospitalId
        ORDER BY r.id DESC
        """)
    List<ReferralRequest> findByPatientIdAndStatusAndHospitalId(
            @Param("patientId") String patientId,
            @Param("status") ReferralStatus status,
            @Param("hospitalId") String hospitalId);

    /** All referrals within a hospital, newest first (admin overview). */
    List<ReferralRequest> findByHospitalIdOrderByIdDesc(String hospitalId);

    // ─── Unscoped Finders (for superadmin/admin with NULL hospitalId) ────────

    /** All referrals with a given status, newest first (no hospital filter). */
    List<ReferralRequest> findByStatusOrderByIdDesc(ReferralStatus status);

    /** All referrals newest first (no hospital filter). */
    List<ReferralRequest> findAllByOrderByIdDesc();

    /** Find a single referral by its PK (no hospital filter). */
    Optional<ReferralRequest> findById(Long id);
}
