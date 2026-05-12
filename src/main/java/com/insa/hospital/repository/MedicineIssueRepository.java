package com.insa.hospital.repository;

import com.insa.hospital.entity.MedicineIssue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicineIssueRepository extends JpaRepository<MedicineIssue, Long> {
    List<MedicineIssue> findByHospitalIdOrderByIdDesc(String hospitalId);
    Page<MedicineIssue> findByHospitalIdOrderByIdDesc(String hospitalId, Pageable pageable);
    Page<MedicineIssue> findByHospitalIdAndIdLikeOrderByIdDesc(String hospitalId, String idLike, Pageable pageable);
}

