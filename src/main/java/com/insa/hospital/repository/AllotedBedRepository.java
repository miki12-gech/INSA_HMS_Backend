package com.insa.hospital.repository;

import com.insa.hospital.entity.AllotedBed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AllotedBedRepository extends JpaRepository<AllotedBed, Long> {
    List<AllotedBed> findByHospitalId(String hospitalId);
    Optional<AllotedBed> findByIdAndHospitalId(Long id, String hospitalId);
    List<AllotedBed> findByPatientAndHospitalId(String patient, String hospitalId);
}
