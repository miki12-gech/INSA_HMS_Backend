package com.insa.hospital.repository;

import com.insa.hospital.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for the `time_schedule` table.
 *
 * NOTE: Table name is `time_schedule` — confirmed from schedule_model.php.
 * The legacy model queries this table by doctor+weekday+hospital_id.
 */
@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Integer> {

    /** All schedules for a hospital — for the admin schedule overview. */
    List<Schedule> findByHospitalId(String hospitalId);

    /** All schedule blocks for a specific doctor. */
    List<Schedule> findByDoctorAndHospitalId(String doctor, String hospitalId);

    /**
     * All schedule blocks for a doctor on a specific weekday.
     * Used for overlap detection before adding a new schedule.
     */
    List<Schedule> findByDoctorAndWeekdayAndHospitalId(String doctor, String weekday, String hospitalId);

    /**
     * Like the above, but EXCLUDING a specific schedule ID.
     * Used during edit to skip checking the current record against itself.
     */
    List<Schedule> findByDoctorAndWeekdayAndHospitalIdAndIdNot(
            String doctor, String weekday, String hospitalId, Integer excludeId);
}
