package com.hospital.hms.repository;

import com.hospital.hms.entity.OtPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OtPaymentRepository extends JpaRepository<OtPayment, Long> {
    List<OtPayment> findByPatient(String patient);
}
