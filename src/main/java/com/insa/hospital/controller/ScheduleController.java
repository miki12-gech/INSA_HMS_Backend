package com.insa.hospital.controller;

import com.insa.hospital.entity.Schedule;
import com.insa.hospital.entity.TimeSlot;
import com.insa.hospital.security.JwtContextHolder;
import com.insa.hospital.service.ScheduleService;
import com.insa.hospital.service.ScheduleService.ScheduleRequestDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller — Doctor Schedule endpoints.
 *
 * Base URL: /api/schedule
 *
 * RBAC (from legacy schedule.php __construct line 14):
 *   Controller access: admin, Doctor, Patient, Nurse, Receptionist
 *   Write (create/edit/delete): admin, Doctor, Receptionist
 *   Read: any authenticated user
 *
 * ══════════════════════════════════════════════════════════════
 *  Endpoints:
 *
 *  GET  /api/schedule                    → All schedules for this hospital
 *  GET  /api/schedule?doctor={id}        → Schedules for a specific doctor
 *  POST /api/schedule                    → Add schedule block (overlap check)
 *  PUT  /api/schedule/{id}               → Edit schedule (re-generates slots)
 *  DELETE /api/schedule/{id}             → Delete schedule + all its slots
 *
 *  GET  /api/schedule/slots?doctor={id}  → All time slots for a doctor
 *  GET  /api/schedule/slots?doctor={id}&weekday=Monday → Slots for a specific day
 * ══════════════════════════════════════════════════════════════
 */
@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @Autowired
    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    // ─── List Schedules ───────────────────────────────────────────────────────

    /**
     * GET /api/schedule
     * GET /api/schedule?doctor={doctorId}
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Schedule>> listSchedules(
            @RequestParam(required = false) String doctor) {
        String hospitalId = JwtContextHolder.getHospitalId();
        List<Schedule> result = (doctor != null && !doctor.isBlank())
                ? scheduleService.listByDoctor(doctor, hospitalId)
                : scheduleService.listByHospital(hospitalId);
        return ResponseEntity.ok(result);
    }

    // ─── List Time Slots ──────────────────────────────────────────────────────

    /**
     * GET /api/schedule/slots?doctor={id}
     * GET /api/schedule/slots?doctor={id}&weekday=Monday
     *
     * Returns the individual bookable time slots for a doctor.
     * Used by the appointment booking form to populate the time dropdown.
     * Slot label: "{sTime} To {eTime}" (matches legacy display format)
     */
    @GetMapping("/slots")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TimeSlot>> listSlots(
            @RequestParam String doctor,
            @RequestParam(required = false) String weekday) {
        String hospitalId = JwtContextHolder.getHospitalId();
        List<TimeSlot> slots = (weekday != null && !weekday.isBlank())
                ? scheduleService.slotsByDoctorAndWeekday(doctor, weekday, hospitalId)
                : scheduleService.slotsByDoctor(doctor, hospitalId);
        return ResponseEntity.ok(slots);
    }

    // ─── Create Schedule ──────────────────────────────────────────────────────

    /**
     * POST /api/schedule
     *
     * Creates a new schedule block for a doctor and auto-generates time slots.
     * Applies the overlap detection algorithm before saving.
     *
     * Body example:
     * {
     *   "doctor": "149",
     *   "weekday": "Monday",
     *   "sTime": "08:00 AM",
     *   "eTime": "05:00 PM",
     *   "duration": "15"
     * }
     *
     * Returns 201 Created with the saved Schedule.
     * Returns 400 Bad Request if overlap detected or invalid times.
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin', 'Doctor', 'Receptionist')")
    public ResponseEntity<Schedule> createSchedule(@RequestBody ScheduleRequestDto dto) {
        String hospitalId = JwtContextHolder.getHospitalId();
        Schedule saved = scheduleService.createSchedule(dto, hospitalId);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ─── Update Schedule ──────────────────────────────────────────────────────

    /**
     * PUT /api/schedule/{id}
     *
     * Updates a schedule block and regenerates its time slots.
     * Validates no overlap with other schedules (excluding self).
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin', 'Doctor', 'Receptionist')")
    public ResponseEntity<Schedule> updateSchedule(
            @PathVariable Integer id,
            @RequestBody ScheduleRequestDto dto) {
        String hospitalId = JwtContextHolder.getHospitalId();
        return ResponseEntity.ok(scheduleService.updateSchedule(id, dto, hospitalId));
    }

    // ─── Delete Schedule ──────────────────────────────────────────────────────

    /**
     * DELETE /api/schedule/{id}
     *
     * Deletes the schedule block AND all its generated time_slot rows.
     * CRITICAL ORDER: time_slots deleted first, then schedule.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin', 'Doctor')")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Integer id) {
        scheduleService.deleteSchedule(id, JwtContextHolder.getHospitalId());
        return ResponseEntity.noContent().build();
    }
}
