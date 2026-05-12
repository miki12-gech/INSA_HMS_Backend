package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity mapping the legacy `time_schedule` table.
 *
 * ══════════════════════════════════════════════════════════════
 *  Table: time_schedule   (NOT 'schedule' — confirmed from
 *  schedule_model.php lines 235, 240, 254, 268, 273)
 * ══════════════════════════════════════════════════════════════
 *
 * A Schedule record defines a BLOCK of working time for a doctor
 * on a specific weekday (e.g., Mon 08:00 AM – 05:00 PM, 15-min slots).
 *
 * When a Schedule is created:
 *  1. This `time_schedule` row is saved (the "master" schedule block).
 *  2. Individual `time_slot` rows are auto-generated based on `duration`
 *     (see TimeSlot.java and ScheduleService.generateSlots()).
 *
 * When a Schedule is deleted:
 *  ALL related time_slot rows for that doctor+weekday MUST be deleted
 *  first (cascade logic in ScheduleService.deleteSchedule()).
 *
 * Columns derived from schedule_model.php insertSchedule() data array:
 *   doctor, s_time, e_time, weekday, s_time_key, duration, hospital_id
 */
@Entity
@Table(name = "time_schedule")
@Getter
@Setter
@NoArgsConstructor
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /**
     * References doctor.id as varchar string.
     * e.g. "149"
     */
    @Column(name = "doctor", length = 100)
    private String doctor;

    /**
     * Start time of the working block (e.g. "08:00 AM").
     * Stored as a human-readable string, matching the legacy
     * 5-minute-increment time array.
     */
    @Column(name = "s_time", length = 100)
    private String sTime;

    /**
     * End time of the working block (e.g. "05:00 PM").
     */
    @Column(name = "e_time", length = 100)
    private String eTime;

    /**
     * Day of week this schedule applies to.
     * Values (from legacy_PHP): "Monday", "Tuesday", "Wednesday",
     * "Thursday", "Friday", "Saturday", "Sunday"
     */
    @Column(name = "weekday", length = 50)
    private String weekday;

    /**
     * Numeric index of s_time within the legacy 5-min array.
     * Used for ordering slots by start time.
     * Calculated at save time from the parsed LocalTime.
     * Range: 0–287 (288 slots × 5 minutes = 24 hours)
     */
    @Column(name = "s_time_key")
    private Integer sTimeKey;

    /**
     * Duration of each appointment slot in minutes.
     * e.g. 15 → slots are 08:00-08:15, 08:15-08:30, etc.
     * Used by ScheduleService.generateSlots() to auto-create TimeSlot records.
     */
    @Column(name = "duration", length = 100)
    private String duration;

    /** Multi-tenant hospital identifier. */
    @Column(name = "hospital_id", length = 100)
    private String hospitalId;
}
