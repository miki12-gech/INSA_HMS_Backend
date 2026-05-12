package com.insa.hospital.repository;

import com.insa.hospital.entity.PharmacyExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PharmacyExpenseCategoryRepository extends JpaRepository<PharmacyExpenseCategory, Long> {
    List<PharmacyExpenseCategory> findByHospitalIdOrderByIdDesc(String hospitalId);
    List<PharmacyExpenseCategory> findByHospitalId(String hospitalId);
}

