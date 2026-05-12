package com.insa.hospital.repository;

import com.insa.hospital.entity.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Integer> {

    Page<Expense> findByHospitalId(String hospitalId, Pageable pageable);

    Page<Expense> findByCategoryAndHospitalId(String category, String hospitalId, Pageable pageable);
}
