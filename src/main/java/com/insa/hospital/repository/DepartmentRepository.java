package com.insa.hospital.repository;

import com.insa.hospital.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Integer> {
    List<Department> findByHospitalId(String hospitalId);
    List<Department> findByHospitalIdIn(List<String> hospitalIds);
}
