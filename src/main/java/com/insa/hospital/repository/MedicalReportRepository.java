package com.insa.hospital.repository;

import com.insa.hospital.entity.MedicalReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalReportRepository extends JpaRepository<MedicalReport, Integer> {

    Page<MedicalReport> findByHospitalId(String hospitalId, Pageable pageable);

    Page<MedicalReport> findByReportTypeAndHospitalId(String reportType, String hospitalId, Pageable pageable);

    Page<MedicalReport> findByPatientAndHospitalId(String patient, String hospitalId, Pageable pageable);

    Page<MedicalReport> findByDoctorAndHospitalId(String doctor, String hospitalId, Pageable pageable);
}
