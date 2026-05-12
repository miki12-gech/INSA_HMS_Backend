package com.hospital.hms.repository;

import com.hospital.hms.entity.TimeSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimeScheduleRepository extends JpaRepository<TimeSchedule, Long> {
    List<TimeSchedule> findByDoctorAndWeekday(String doctor, String weekday);
    List<TimeSchedule> findByDoctor(String doctor);
}
