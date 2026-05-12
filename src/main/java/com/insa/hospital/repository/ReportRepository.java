package com.insa.hospital.repository;

import com.insa.hospital.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    
    List<Report> findByHospitalId(String hospitalId);
    
    List<Report> findByHospitalIdAndReportType(String hospitalId, String reportType);
    
    List<Report> findByPatientAndHospitalId(String patient, String hospitalId);
}
