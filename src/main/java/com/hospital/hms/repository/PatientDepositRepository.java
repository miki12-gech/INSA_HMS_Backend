package com.hospital.hms.repository;

import com.hospital.hms.entity.PatientDeposit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientDepositRepository extends JpaRepository<PatientDeposit, Long> {
    List<PatientDeposit> findByPatient(String patient);
}
