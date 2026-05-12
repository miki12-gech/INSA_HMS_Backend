package com.insa.hospital.repository;

import com.insa.hospital.entity.Sms;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SmsRepository extends JpaRepository<Sms, Long> {
    List<Sms> findByHospitalIdOrderByIdDesc(String hospitalId);
    List<Sms> findByHospitalIdAndUserOrderByIdDesc(String hospitalId, String user);
}

