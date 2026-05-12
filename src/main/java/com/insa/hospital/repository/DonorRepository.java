package com.insa.hospital.repository;

import com.insa.hospital.entity.Donor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DonorRepository extends JpaRepository<Donor, Integer> {

    Page<Donor> findByHospitalId(String hospitalId, Pageable pageable);

    Page<Donor> findByGroupAndHospitalId(String group, String hospitalId, Pageable pageable);
}
