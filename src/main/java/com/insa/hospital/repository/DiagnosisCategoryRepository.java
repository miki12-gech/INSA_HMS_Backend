package com.insa.hospital.repository;

import com.insa.hospital.entity.DiagnosisCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiagnosisCategoryRepository extends JpaRepository<DiagnosisCategory, Long> {
    List<DiagnosisCategory> findByCategoryContainingIgnoreCase(String category);
    Optional<DiagnosisCategory> findByCategoryIgnoreCase(String category);
}
