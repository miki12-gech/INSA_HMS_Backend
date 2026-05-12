package com.insa.hospital.repository;

import com.insa.hospital.entity.SmsSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SmsSettingsRepository extends JpaRepository<SmsSettings, Long> {
    List<SmsSettings> findByHospitalId(String hospitalId);
    Optional<SmsSettings> findByHospitalIdAndName(String hospitalId, String name);
}

