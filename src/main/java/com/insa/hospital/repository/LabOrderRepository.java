package com.insa.hospital.repository;

import com.insa.hospital.entity.LabOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LabOrderRepository extends JpaRepository<LabOrder, Integer> {
    List<LabOrder> findByStatus(String status);
    List<LabOrder> findByPatientAndDateString(String patient, String dateString);
}
