package com.insa.hospital.repository;

import com.insa.hospital.entity.Laboratorist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LaboratoristRepository extends JpaRepository<Laboratorist, Integer> {

    Page<Laboratorist> findByHospitalId(String hospitalId, Pageable pageable);
}
