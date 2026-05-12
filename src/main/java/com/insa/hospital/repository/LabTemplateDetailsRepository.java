package com.insa.hospital.repository;

import com.insa.hospital.entity.LabTemplateDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LabTemplateDetailsRepository extends JpaRepository<LabTemplateDetails, Long> {
    List<LabTemplateDetails> findByLabId(String labId);
    void deleteByLabId(String labId);
}
