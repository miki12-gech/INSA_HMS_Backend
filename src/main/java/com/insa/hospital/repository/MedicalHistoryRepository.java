package com.insa.hospital.repository;

import com.insa.hospital.entity.MedicalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalHistoryRepository extends JpaRepository<MedicalHistory, Integer> {
    List<MedicalHistory> findByPatientId(String patientId);
    List<MedicalHistory> findByPatientIdAndHospitalId(String patientId, String hospitalId);

    @Query(value = """
        SELECT * FROM medical_history
        WHERE hospital_id = :hospitalId
          AND COALESCE(TRIM(diagnosis_category), '') <> ''
          AND CASE
                WHEN COALESCE(TRIM(date), '') ~ '^[0-9]+$'
                  THEN CAST(TRIM(date) AS BIGINT)
                ELSE 0
              END BETWEEN :fromEpoch AND :toEpoch
        ORDER BY CASE
                   WHEN COALESCE(TRIM(date), '') ~ '^[0-9]+$'
                     THEN CAST(TRIM(date) AS BIGINT)
                   ELSE 0
                 END DESC
        """, nativeQuery = true)
    List<MedicalHistory> findDiagnosisHistoryByHospitalIdAndDateRange(
            @Param("hospitalId") String hospitalId,
            @Param("fromEpoch") long fromEpoch,
            @Param("toEpoch") long toEpoch);
}
