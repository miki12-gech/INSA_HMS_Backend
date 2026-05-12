package com.insa.hospital.repository;

import com.insa.hospital.entity.ServiceCharge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceChargeRepository extends JpaRepository<ServiceCharge, Long> {
    List<ServiceCharge> findByHospitalIdOrderByIdDesc(String hospitalId);
    List<ServiceCharge> findByHospitalIdAndPatientIdOrderByIdDesc(String hospitalId, String patientId);
    List<ServiceCharge> findByHospitalIdAndVisitIdOrderByIdDesc(String hospitalId, String visitId);
}
