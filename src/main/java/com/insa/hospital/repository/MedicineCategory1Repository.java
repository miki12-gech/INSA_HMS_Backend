package com.insa.hospital.repository;

import com.insa.hospital.entity.MedicineCategory1;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicineCategory1Repository extends JpaRepository<MedicineCategory1, Integer> {
    List<MedicineCategory1> findByHospitalId(String hospitalId);
}
