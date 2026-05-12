package com.hospital.hms.controller;

import com.hospital.hms.entity.Holiday;
import com.hospital.hms.entity.TimeSchedule;
import com.hospital.hms.entity.TimeSlot;
import com.hospital.hms.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping
    public ResponseEntity<List<TimeSchedule>> getAllSchedules() {
        return ResponseEntity.ok(scheduleService.getSchedules());
    }

    @GetMapping("/doctor/{doctor}")
    public ResponseEntity<List<TimeSchedule>> getSchedulesByDoctor(@PathVariable String doctor) {
        return ResponseEntity.ok(scheduleService.getScheduleByDoctor(doctor));
    }

    @PostMapping
    public ResponseEntity<?> addSchedule(@RequestBody TimeSchedule schedule) {
        try {
            scheduleService.addSchedule(schedule);
            return ResponseEntity.ok().body("Schedule Added/Updated Successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping
    public ResponseEntity<?> deleteSchedule(@RequestParam Long id, @RequestParam String doctor, @RequestParam String weekday) {
        scheduleService.deleteSchedule(id, doctor, weekday);
        return ResponseEntity.ok().body("Deleted Successfully");
    }

    @GetMapping("/slots/{doctor}")
    public ResponseEntity<List<TimeSlot>> getTimeSlotsByDoctor(@PathVariable String doctor) {
        return ResponseEntity.ok(scheduleService.getTimeSlotsByDoctor(doctor));
    }

    @GetMapping("/holiday")
    public ResponseEntity<List<Holiday>> getAllHolidays() {
        return ResponseEntity.ok(scheduleService.getHolidays());
    }

    @GetMapping("/holiday/doctor/{doctor}")
    public ResponseEntity<List<Holiday>> getHolidaysByDoctor(@PathVariable String doctor) {
        return ResponseEntity.ok(scheduleService.getHolidaysByDoctor(doctor));
    }

    @PostMapping("/holiday")
    public ResponseEntity<?> addHoliday(@RequestBody Holiday holiday) {
        scheduleService.addHoliday(holiday);
        return ResponseEntity.ok().body("Holiday Added Successfully");
    }

    @DeleteMapping("/holiday/{id}")
    public ResponseEntity<?> deleteHoliday(@PathVariable Long id) {
        scheduleService.deleteHoliday(id);
        return ResponseEntity.ok().body("Holiday Deleted Successfully");
    }
}
