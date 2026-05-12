package com.insa.hospital.repository;

import com.insa.hospital.entity.HospitalSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for the `settings` table.
 *
 * AUDIT FIX — Alert 1: The legacy settings_model.php queries:
 *   SELECT * FROM settings WHERE hospital_id = ?
 *
 * The primary lookup is always by hospital_id (not by pk id),
 * matching the legacy settings_model.getSettings() behaviour.
 */
@Repository
public interface HospitalSettingsRepository extends JpaRepository<HospitalSettings, Integer> {
    Optional<HospitalSettings> findByHospitalId(String hospitalId);
}
