package com.insa.hospital.repository;

import com.insa.hospital.entity.Pharmacist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PharmacistRepository extends JpaRepository<Pharmacist, Integer> {

    Page<Pharmacist> findByHospitalId(String hospitalId, Pageable pageable);
}
