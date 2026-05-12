package com.insa.hospital.repository;

import com.insa.hospital.entity.PharmacyPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PharmacyPaymentRepository extends JpaRepository<PharmacyPayment, Long> {
    List<PharmacyPayment> findByHospitalIdOrderByIdDesc(String hospitalId);
    Page<PharmacyPayment> findByHospitalIdOrderByIdDesc(String hospitalId, Pageable pageable);
    List<PharmacyPayment> findByHospitalIdAndDateBetween(String hospitalId, String fromDate, String toDate);
    List<PharmacyPayment> findByHospitalIdAndPatientOrderByIdDesc(String hospitalId, String patientId);
    Page<PharmacyPayment> findByHospitalIdAndIdLikeOrderByIdAsc(String hospitalId, String idLike, Pageable pageable);
}

