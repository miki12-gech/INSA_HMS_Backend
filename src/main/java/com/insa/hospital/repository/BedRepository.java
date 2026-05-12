package com.insa.hospital.repository;

import com.insa.hospital.entity.Bed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BedRepository extends JpaRepository<Bed, Long> {
    List<Bed> findByHospitalId(String hospitalId);
    Optional<Bed> findByIdAndHospitalId(Long id, String hospitalId);
    Optional<Bed> findByBedIdAndHospitalId(String bedId, String hospitalId);
}
