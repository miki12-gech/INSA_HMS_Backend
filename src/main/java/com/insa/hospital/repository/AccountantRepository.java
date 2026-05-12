package com.insa.hospital.repository;

import com.insa.hospital.entity.Accountant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountantRepository extends JpaRepository<Accountant, Integer> {

    Page<Accountant> findByHospitalId(String hospitalId, Pageable pageable);
}
