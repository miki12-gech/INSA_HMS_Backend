package com.hospital.hms.controller;

import com.hospital.hms.dto.AppointmentDto;
import com.hospital.hms.entity.Appointment;
import com.hospital.hms.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointment")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping
    public ResponseEntity<List<Appointment>> getAllAppointments() {
        return ResponseEntity.ok(appointmentService.getAllAppointments());
    }

    @GetMapping("/doctor/{doctor}")
    public ResponseEntity<List<Appointment>> getAppointmentsByDoctor(@PathVariable String doctor) {
        return ResponseEntity.ok(appointmentService.getAppointmentsByDoctor(doctor));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Appointment>> getAppointmentsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(appointmentService.getAppointmentsByStatus(status));
    }

    @GetMapping("/request")
    public ResponseEntity<List<Appointment>> getRequestedAppointments() {
        return ResponseEntity.ok(appointmentService.getRequestedAppointments());
    }

    @PostMapping
    public ResponseEntity<?> addAppointment(@RequestBody AppointmentDto dto) {
        try {
            appointmentService.addAppointment(dto);
            return ResponseEntity.ok().body("Appointment Added/Updated Successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAppointment(@PathVariable Long id) {
        appointmentService.deleteAppointment(id);
        return ResponseEntity.ok().body("Appointment Deleted Successfully");
    }
}
