package com.insa.hospital.repository;

import com.insa.hospital.entity.Email;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface EmailRepository extends JpaRepository<Email, Long> {
    List<Email> findByHospitalIdOrderByIdDesc(String hospitalId);
    List<Email> findByHospitalIdAndUserOrderByIdDesc(String hospitalId, String user);
    Page<Email> findByHospitalId(String hospitalId, Pageable pageable);
}

