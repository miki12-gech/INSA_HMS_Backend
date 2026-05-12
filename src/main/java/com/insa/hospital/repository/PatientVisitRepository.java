package com.insa.hospital.repository;

import com.insa.hospital.entity.PatientVisit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface PatientVisitRepository extends JpaRepository<PatientVisit, Long> {

    @Query(value = """
        SELECT * FROM patient_visit
        WHERE hospital_id = :hospitalId
        AND CASE WHEN created_at ~ '^[0-9]+$' THEN CAST(created_at AS BIGINT) ELSE NULL END
            BETWEEN :startEpoch AND :endEpoch
        ORDER BY id DESC
        """, nativeQuery = true)
    List<PatientVisit> findTodayVisits(@Param("hospitalId") String hospitalId,
                                       @Param("startEpoch") long startEpoch,
                                       @Param("endEpoch") long endEpoch);

    @Query(value = """
        SELECT * FROM patient_visit
        WHERE hospital_id = :hospitalId
        AND CASE WHEN created_at ~ '^[0-9]+$' THEN CAST(created_at AS BIGINT) ELSE NULL END
            BETWEEN :startEpoch AND :endEpoch
        ORDER BY id DESC
        """, nativeQuery = true)
    List<PatientVisit> findVisitsByHospitalIdAndCreatedAtBetween(
            @Param("hospitalId") String hospitalId,
            @Param("startEpoch") long startEpoch,
            @Param("endEpoch") long endEpoch);
}
