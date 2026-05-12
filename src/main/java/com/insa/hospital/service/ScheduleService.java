package com.insa.hospital.service;

import com.insa.hospital.entity.Schedule;
import com.insa.hospital.entity.TimeSlot;
import com.insa.hospital.exception.ResourceNotFoundException;
import com.insa.hospital.repository.ScheduleRepository;
import com.insa.hospital.repository.TimeSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Schedule Service — Business Logic Layer
 *
 * ══════════════════════════════════════════════════════════════
 *  AUDIT FIX — Missing Feature 1 & Alert 2:
 *  Doctor Schedule Module with Overlap Detection
 * ══════════════════════════════════════════════════════════════
 *
 * Responsibilities:
 *  1. Overlap detection: Reject a new schedule if its time range
 *     overlaps any existing schedule for that doctor+weekday.
 *  2. Slot generation: After saving a new schedule, auto-generate
 *     individual TimeSlot records (every `duration` minutes).
 *  3. Cascade delete: When deleting a schedule block, first delete
 *     all time_slot rows for that doctor+weekday.
 *
 * ══════════════════════════════════════════════════════════════
 *  OVERLAP DETECTION — Java Implementation of Legacy Algorithm
 * ══════════════════════════════════════════════════════════════
 *
 * The legacy PHP code (schedule.php lines 368–426) uses a 288-element
 * String[] array (one entry per 5-minute increment across 24 hours)
 * and compares array indexes to detect overlap. The array had a known
 * AM/PM bug (e.g. index 0 = "12:00 PM" but index 1 = "12:05 AM").
 *
 * This Java implementation is SEMANTICALLY EQUIVALENT but correct:
 *  - Parse s_time / e_time strings as LocalTime (handles AM/PM)
 *  - Convert LocalTime → minutes-since-midnight integer (the index)
 *  - Apply the same overlap logic: newStart < existingEnd AND newEnd > existingStart
 *
 * The s_time_key stored in the DB is: LocalTime.toSecondOfDay() / 60
 * (minutes since midnight — equivalent to the array index × 5min increment).
 *
 * Time format accepted: "hh:mm a" e.g. "08:00 AM", "05:00 PM"
 * ══════════════════════════════════════════════════════════════
 *
 * ══════════════════════════════════════════════════════════════
 *  SLOT GENERATION ALGORITHM
 * ══════════════════════════════════════════════════════════════
 *
 * Given: s_time="08:00 AM", e_time="05:00 PM", duration=15 (minutes)
 * The service generates time_slot rows:
 *   08:00 AM → 08:15 AM   (s_time_key = 480)
 *   08:15 AM → 08:30 AM   (s_time_key = 495)
 *   ...until slotEnd >= eTime schedule boundary
 *
 * The slot label in the appointment form is:
 *   "{slot.sTime} To {slot.eTime}"
 * ══════════════════════════════════════════════════════════════
 */
@Service
@Transactional
public class ScheduleService {

    // The format used in the legacy PHP time-array and stored in the DB
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");

    private final ScheduleRepository scheduleRepository;
    private final TimeSlotRepository timeSlotRepository;

    @Autowired
    public ScheduleService(ScheduleRepository scheduleRepository,
                           TimeSlotRepository timeSlotRepository) {
        this.scheduleRepository = scheduleRepository;
        this.timeSlotRepository = timeSlotRepository;
    }

    // ─── Create Schedule + Generate Slots ──────────────────────────────────────

    /**
     * Creates a schedule block for a doctor on a weekday.
     *
     * Steps:
     *  1. Validate start < end
     *  2. Run overlap detection against existing schedules
     *  3. Save the time_schedule record
     *  4. Auto-generate time_slot records
     *
     * @param dto        ScheduleRequestDto from controller
     * @param hospitalId from JWT
     * @throws IllegalArgumentException if time order is invalid or overlap detected
     */
    @Transactional
    public Schedule createSchedule(ScheduleRequestDto dto, String hospitalId) {
        String doctor  = dto.doctor();
        String weekday = dto.weekday();
        String sTime   = dto.sTime();
        String eTime   = dto.eTime();
        int durationMins = parseIntSafe(dto.duration(), 15);

        // 1. Parse and validate time order
        LocalTime start = parseTime(sTime);
        LocalTime end   = parseTime(eTime);

        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Start time must be before end time.");
        }

        // 2. Overlap detection — check all existing schedules for this doctor+weekday
        List<Schedule> existing = scheduleRepository
                .findByDoctorAndWeekdayAndHospitalId(doctor, weekday, hospitalId);
        checkOverlap(start, end, existing, null);

        // 3. Build and save the time_schedule record
        Schedule schedule = new Schedule();
        schedule.setDoctor(doctor);
        schedule.setWeekday(weekday);
        schedule.setSTime(formatTime(start));
        schedule.setETime(formatTime(end));
        schedule.setSTimeKey(toMinutes(start));
        schedule.setDuration(String.valueOf(durationMins));
        schedule.setHospitalId(hospitalId);
        Schedule saved = scheduleRepository.save(schedule);

        // 4. Auto-generate individual time_slot records
        generateSlots(doctor, weekday, start, end, durationMins, hospitalId);

        return saved;
    }

    /**
     * Updates an existing schedule block.
     * Validates overlap excluding the current schedule ID.
     * Regenerates time slots: deletes old ones then creates fresh ones.
     */
    @Transactional
    public Schedule updateSchedule(Integer id, ScheduleRequestDto dto, String hospitalId) {
        Schedule schedule = scheduleRepository.findById(id)
                .filter(s -> hospitalId.equals(s.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", id));

        LocalTime start = parseTime(dto.sTime());
        LocalTime end   = parseTime(dto.eTime());
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Start time must be before end time.");
        }

        // Overlap check excluding this schedule's own ID
        List<Schedule> existing = scheduleRepository
                .findByDoctorAndWeekdayAndHospitalIdAndIdNot(
                        schedule.getDoctor(), dto.weekday(), hospitalId, id);
        checkOverlap(start, end, existing, null);

        // Regenerate time slots
        timeSlotRepository.deleteByDoctorAndWeekdayAndHospitalId(
                schedule.getDoctor(), schedule.getWeekday(), hospitalId);

        int durationMins = parseIntSafe(dto.duration(), parseIntSafe(schedule.getDuration(), 15));
        schedule.setSTime(formatTime(start));
        schedule.setETime(formatTime(end));
        schedule.setSTimeKey(toMinutes(start));
        schedule.setWeekday(dto.weekday());
        schedule.setDuration(String.valueOf(durationMins));
        Schedule saved = scheduleRepository.save(schedule);

        generateSlots(schedule.getDoctor(), dto.weekday(), start, end, durationMins, hospitalId);
        return saved;
    }

    // ─── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Schedule> listByHospital(String hospitalId) {
        return scheduleRepository.findByHospitalId(hospitalId);
    }

    @Transactional(readOnly = true)
    public List<Schedule> listByDoctor(String doctor, String hospitalId) {
        return scheduleRepository.findByDoctorAndHospitalId(doctor, hospitalId);
    }

    @Transactional(readOnly = true)
    public List<TimeSlot> slotsByDoctor(String doctor, String hospitalId) {
        return timeSlotRepository.findByDoctorAndHospitalIdOrderBySTimeKeyAsc(doctor, hospitalId);
    }

    @Transactional(readOnly = true)
    public List<TimeSlot> slotsByDoctorAndWeekday(String doctor, String weekday, String hospitalId) {
        return timeSlotRepository.findByDoctorAndWeekdayAndHospitalIdOrderBySTimeKeyAsc(
                doctor, weekday, hospitalId);
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    /**
     * Deletes a schedule block AND all its generated time_slot records.
     *
     * CRITICAL ORDER:
     *  1. Delete time_slot rows for doctor+weekday FIRST
     *  2. Then delete the time_schedule row
     *
     * Mirrors: schedule.php deleteSchedule() which calls:
     *   $this->schedule_model->deleteTimeSlotByDoctorByWeekday($doctor, $weekday)
     *   $this->schedule_model->deleteSchedule($id)
     */
    @Transactional
    public void deleteSchedule(Integer id, String hospitalId) {
        Schedule schedule = scheduleRepository.findById(id)
                .filter(s -> hospitalId.equals(s.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", id));

        // 1. Delete all time_slots for this doctor+weekday (cascade, not FK)
        timeSlotRepository.deleteByDoctorAndWeekdayAndHospitalId(
                schedule.getDoctor(), schedule.getWeekday(), hospitalId);

        // 2. Delete the schedule block
        scheduleRepository.delete(schedule);
    }

    // ─── Private: Overlap Detection ───────────────────────────────────────────

    /**
     * Throws IllegalArgumentException if [newStart, newEnd) overlaps any existing schedule.
     *
     * ══════════════════════════════════════════════════
     *  Algorithm (Java equiv. of legacy PHP index logic):
     *
     *  Two ranges [A,B) and [C,D) overlap if:
     *    A < D  AND  B > C
     *
     *  This correctly handles all overlap cases:
     *    - Partial overlap at start
     *    - Partial overlap at end
     *    - One contains the other
     *    - Identical ranges
     *
     *  Non-overlapping: A >= D OR B <= C
     *  (i.e. new block ends before existing starts, or starts after existing ends)
     * ══════════════════════════════════════════════════
     */
    private void checkOverlap(LocalTime newStart, LocalTime newEnd,
                               List<Schedule> existingList, Integer excludeId) {
        for (Schedule ex : existingList) {
            if (excludeId != null && excludeId.equals(ex.getId())) continue;

            LocalTime exStart = parseTime(ex.getSTime());
            LocalTime exEnd   = parseTime(ex.getETime());

            // Overlap condition
            if (newStart.isBefore(exEnd) && newEnd.isAfter(exStart)) {
                throw new IllegalArgumentException(
                    String.format("Time slot overlaps with existing schedule [%s - %s] on %s.",
                        ex.getSTime(), ex.getETime(), ex.getWeekday())
                );
            }
        }
    }

    // ─── Private: Slot Generation ─────────────────────────────────────────────

    /**
     * Generates individual time_slot rows within the schedule window.
     *
     * Algorithm:
     *  slotStart = scheduleStart
     *  while slotStart + duration <= scheduleEnd:
     *    slotEnd = slotStart + duration
     *    save TimeSlot(sTime=slotStart, eTime=slotEnd, s_time_key=minutes(slotStart))
     *    slotStart = slotEnd
     */
    private void generateSlots(String doctor, String weekday,
                                LocalTime scheduleStart, LocalTime scheduleEnd,
                                int durationMins, String hospitalId) {
        List<TimeSlot> slots = new ArrayList<>();
        LocalTime slotStart = scheduleStart;

        while (true) {
            LocalTime slotEnd = slotStart.plusMinutes(durationMins);
            if (slotEnd.isAfter(scheduleEnd)) break;

            TimeSlot slot = new TimeSlot();
            slot.setDoctor(doctor);
            slot.setWeekday(weekday);
            slot.setSTime(formatTime(slotStart));
            slot.setETime(formatTime(slotEnd));
            slot.setSTimeKey(toMinutes(slotStart));
            slot.setHospitalId(hospitalId);
            slots.add(slot);

            slotStart = slotEnd;
        }

        if (!slots.isEmpty()) {
            timeSlotRepository.saveAll(slots);
        }
    }

    // ─── Private: Time Utilities ──────────────────────────────────────────────

    /** Parse "08:00 AM" or "08:00" as LocalTime. */
    private LocalTime parseTime(String timeStr) {
        if (!StringUtils.hasText(timeStr)) {
            throw new IllegalArgumentException("Time value cannot be empty.");
        }
        try {
            // Try "hh:mm a" (12-hour with AM/PM) first
            return LocalTime.parse(timeStr.trim().toUpperCase(), TIME_FMT);
        } catch (Exception e) {
            try {
                // Fallback: try "HH:mm" (24-hour)
                return LocalTime.parse(timeStr.trim());
            } catch (Exception ex) {
                throw new IllegalArgumentException("Cannot parse time: '" + timeStr + "'. Expected format: 'hh:mm AM'");
            }
        }
    }

    /** Format LocalTime as "hh:mm a" → "08:00 AM". */
    private String formatTime(LocalTime t) {
        return t.format(TIME_FMT);
    }

    /**
     * Converts LocalTime to minutes-since-midnight.
     * This is the Java equivalent of the legacy PHP array index.
     * e.g. 08:00 AM → 480, 05:00 PM → 1020
     */
    private int toMinutes(LocalTime t) {
        return t.getHour() * 60 + t.getMinute();
    }

    private int parseIntSafe(String s, int defaultValue) {
        if (!StringUtils.hasText(s)) return defaultValue;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return defaultValue; }
    }

    // ─── Inner DTO (avoids separate file for a simple module) ─────────────────

    /**
     * ScheduleRequestDto — immutable record for schedule creation/update.
     * Accepts time strings in "hh:mm AM" format to match the legacy PHP form.
     */
    public record ScheduleRequestDto(
            String doctor,
            String weekday,
            String sTime,
            String eTime,
            String duration
    ) {}
}
