package com.insa.hospital.repository;

import com.insa.hospital.entity.Lab;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LabRepository extends JpaRepository<Lab, Long> {
    List<Lab> findByPatient(String patient);
    List<Lab> findByDoctor(String doctor);
    List<Lab> findByDateString(String dateString);
    Page<Lab> findByStatusAndHospitalId(String status, String hospitalId, Pageable pageable);

    @Query("""
        SELECT l FROM Lab l
        WHERE l.hospitalId IN :hospitalIds
           OR (:includeBlank = true AND COALESCE(TRIM(l.hospitalId), '') = '')
        ORDER BY l.id DESC
        """)
    List<Lab> findVisibleByHospitalIdsOrderByIdDesc(
            @Param("hospitalIds") List<String> hospitalIds,
            @Param("includeBlank") boolean includeBlank);
}
