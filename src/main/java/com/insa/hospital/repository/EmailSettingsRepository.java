package com.insa.hospital.repository;

import com.insa.hospital.entity.EmailSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailSettingsRepository extends JpaRepository<EmailSettings, Long> {
    Optional<EmailSettings> findByHospitalId(String hospitalId);
}

