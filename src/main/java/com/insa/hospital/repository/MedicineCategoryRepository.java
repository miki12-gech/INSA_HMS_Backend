package com.insa.hospital.repository;

import com.insa.hospital.entity.MedicineCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MedicineCategoryRepository extends JpaRepository<MedicineCategory, Integer> {
    List<MedicineCategory> findByHospitalId(String hospitalId);
}
