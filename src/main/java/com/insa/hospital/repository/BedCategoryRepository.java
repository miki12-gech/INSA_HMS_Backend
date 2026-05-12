package com.insa.hospital.repository;

import com.insa.hospital.entity.BedCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BedCategoryRepository extends JpaRepository<BedCategory, Long> {
    List<BedCategory> findByHospitalId(String hospitalId);
    Optional<BedCategory> findByIdAndHospitalId(Long id, String hospitalId);
}
