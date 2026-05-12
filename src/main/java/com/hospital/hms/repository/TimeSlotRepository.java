package com.hospital.hms.repository;

import com.hospital.hms.entity.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    List<TimeSlot> findByDoctorAndWeekdayOrderBySTimeKeyAsc(String doctor, String weekday);
    List<TimeSlot> findByDoctorOrderBySTimeKeyAsc(String doctor);
    void deleteByDoctorAndWeekday(String doctor, String weekday);
}
