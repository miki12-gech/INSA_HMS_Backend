package com.insa.hospital.repository;

import com.insa.hospital.entity.AllianceHospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AllianceHospitalRepository extends JpaRepository<AllianceHospital, Long> {

    List<AllianceHospital> findByHospitalIdOrderByNameAsc(String hospitalId);

    List<AllianceHospital> findByHospitalIdAndStatusOrderByNameAsc(String hospitalId, String status);

    List<AllianceHospital> findAllByOrderByNameAsc();

    List<AllianceHospital> findByStatusOrderByNameAsc(String status);
}
