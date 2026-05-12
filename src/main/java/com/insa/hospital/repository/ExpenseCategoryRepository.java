package com.insa.hospital.repository;

import com.insa.hospital.entity.ExpenseCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Integer> {

    Page<ExpenseCategory> findByHospitalId(String hospitalId, Pageable pageable);

    List<ExpenseCategory> findByHospitalId(String hospitalId);
}
