package com.insa.hospital.repository;

import com.insa.hospital.entity.OtPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OtPaymentRepository extends JpaRepository<OtPayment, Integer> {

    Page<OtPayment> findByHospitalId(String hospitalId, Pageable pageable);

    List<OtPayment> findByPatientAndHospitalId(String patient, String hospitalId);

    Page<OtPayment> findByStatusAndHospitalId(String status, String hospitalId, Pageable pageable);
}
