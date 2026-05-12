package com.insa.hospital.repository;

import com.insa.hospital.entity.LabCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LabCategoryRepository extends JpaRepository<LabCategory, Long> {
}
