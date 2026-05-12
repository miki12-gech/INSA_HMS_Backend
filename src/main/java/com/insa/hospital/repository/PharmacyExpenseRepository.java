package com.insa.hospital.repository;

import com.insa.hospital.entity.PharmacyExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PharmacyExpenseRepository extends JpaRepository<PharmacyExpense, Long> {
    List<PharmacyExpense> findByHospitalIdOrderByIdDesc(String hospitalId);
    List<PharmacyExpense> findByHospitalIdAndDateBetween(String hospitalId, String fromDate, String toDate);
}

