package com.insa.hospital.repository;

import com.insa.hospital.entity.PatientDeposit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for the `patient_deposit` table.
 *
 * AUDIT FIX — Alert 5: Every payment creation must also write a
 * patient_deposit record. This repository is injected into
 * PaymentService to perform the atomic double-write.
 */
@Repository
public interface PatientDepositRepository extends JpaRepository<PatientDeposit, Integer> {

    /**
     * Find a deposit by its composite business key.
     * Used to check if an "initial" deposit already exists
     * before adding a subsequent partial payment deposit.
     *
     * Example: findByAmountReceivedId("127.gp")
     */
    Optional<PatientDeposit> findByAmountReceivedId(String amountReceivedId);

    /** All deposits linked to a specific invoice (payment_id). */
    List<PatientDeposit> findByPaymentId(String paymentId);

    /** All deposits for a specific patient (used in patient billing history). */
    List<PatientDeposit> findByPatient(String patient);
}
