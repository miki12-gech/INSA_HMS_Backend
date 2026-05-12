package com.insa.hospital.repository;

import com.insa.hospital.entity.PatientNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientNoteRepository extends JpaRepository<PatientNote, Integer> {
    List<PatientNote> findByPatientId(String patientId);
    List<PatientNote> findByPatientIdAndHospitalId(String patientId, String hospitalId);
}
