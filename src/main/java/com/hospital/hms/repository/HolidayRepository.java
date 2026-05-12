package com.hospital.hms.repository;

import com.hospital.hms.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    List<Holiday> findByDate(String date);
    List<Holiday> findByDoctorOrderByDateAsc(String doctor);
    Optional<Holiday> findByDoctorAndDate(String doctor, String date);
}
