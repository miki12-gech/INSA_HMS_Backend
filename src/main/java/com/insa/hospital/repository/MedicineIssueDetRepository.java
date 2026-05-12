package com.insa.hospital.repository;

import com.insa.hospital.entity.MedicineIssueDet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicineIssueDetRepository extends JpaRepository<MedicineIssueDet, Long> {
    List<MedicineIssueDet> findByMedicineIssueIdOrderByIdDesc(Integer medicineIssueId);
    List<MedicineIssueDet> findByMedicineId(String medicineId);
    void deleteByMedicineIssueId(Integer medicineIssueId);
}

