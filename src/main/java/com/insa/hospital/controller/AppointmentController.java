package com.insa.hospital.controller;

import com.insa.hospital.dto.AppointmentRequestDto;
import com.insa.hospital.dto.AppointmentResponseDto;
import com.insa.hospital.dto.CalendarEventDto;
import com.insa.hospital.security.JwtContextHolder;
import com.insa.hospital.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Appointment REST Controller
 * Base URL: /api/appointments
 *
 * Roles from legacy groups table (agent.md §8):
 *  - View: Doctor, Nurse, Receptionist, admin, superadmin
 *  - Book/Update: Receptionist, Doctor, admin, superadmin
 *  - Delete: admin, superadmin
 */
@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @Autowired
    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    // ─── POST /api/appointments ───────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyAuthority('Receptionist', 'Doctor', 'admin', 'superadmin')")
    public ResponseEntity<AppointmentResponseDto> bookAppointment(
            @Valid @RequestBody AppointmentRequestDto dto,
            Authentication authentication) {

        String hospitalId    = JwtContextHolder.getHospitalId();
        String bookedByUserId = JwtContextHolder.getUserId();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appointmentService.bookAppointment(dto, hospitalId, bookedByUserId));
    }

    // ─── GET /api/appointments ────────────────────────────────────────────────

    /**
     * Paginated list of appointments.
     * Filters: ?doctor=id, ?patient=id, ?status=..., ?search=..., ?date=epoch
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('Receptionist', 'Doctor', 'Nurse', 'admin', 'superadmin')")
    public ResponseEntity<Page<AppointmentResponseDto>> listAppointments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String doctor,
            @RequestParam(required = false) String patient,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            Authentication authentication) {

        String hospitalId = JwtContextHolder.getHospitalId();
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<AppointmentResponseDto> result;

        if (StringUtils.hasText(search)) {
            result = appointmentService.search(search, hospitalId, pageable);
        } else if (StringUtils.hasText(doctor)) {
            result = appointmentService.listByDoctor(doctor, hospitalId, pageable);
        } else if (StringUtils.hasText(patient)) {
            result = appointmentService.listByPatient(patient, hospitalId, pageable);
        } else if (StringUtils.hasText(status)) {
            result = appointmentService.listByStatus(status, hospitalId, pageable);
        } else {
            result = appointmentService.listAppointments(hospitalId, pageable);
        }

        return ResponseEntity.ok(result);
    }

    // ─── GET /api/appointments/datatables ─────────────────────────────────────

    /**
     * Legacy DataTables endpoint replicating exactly the PHP getAppointment() response structure.
     * Expects parameters: draw, start, length, search[value]
     */
    @GetMapping(value = {"/datatables", "/getAppointment"})
    @PreAuthorize("hasAnyAuthority('Receptionist', 'Doctor', 'Nurse', 'admin', 'superadmin')")
    public ResponseEntity<com.insa.hospital.dto.DataTablesResponseDto> getAppointmentsDataTables(
            @RequestParam(name = "draw", defaultValue = "1") int draw,
            @RequestParam(name = "start", defaultValue = "0") int start,
            @RequestParam(name = "length", defaultValue = "10") int length,
            @RequestParam(name = "search[value]", required = false) String search,
            Authentication authentication) {

        String hospitalId = JwtContextHolder.getHospitalId();
        return ResponseEntity.ok(
                appointmentService.listAppointmentsDataTables(hospitalId, draw, start, length, search));
    }

    // ─── GET /api/appointments/by-doctor-date ─────────────────────────────────

    /**
     * List all appointments for a doctor on a specific date.
     * Used by the scheduling / calendar view.
     * ?doctorId=149&date=1640041200
     */
    @GetMapping("/by-doctor-date")
    @PreAuthorize("hasAnyAuthority('Receptionist', 'Doctor', 'Nurse', 'admin', 'superadmin')")
    public ResponseEntity<List<AppointmentResponseDto>> listByDoctorAndDate(
            @RequestParam String doctorId,
            @RequestParam String date,
            Authentication authentication) {

        String hospitalId = JwtContextHolder.getHospitalId();
        return ResponseEntity.ok(
                appointmentService.listByDoctorAndDate(doctorId, date, hospitalId));
    }

    // ─── GET /api/appointments/calendar ───────────────────────────────────────

    /**
     * Replicates `getAppointmentByJason` logic.
     * Returns a JSON array explicitly mapped for fullcalendar.io
     */
    @GetMapping("/calendar")
    @PreAuthorize("hasAnyAuthority('Receptionist', 'Doctor', 'Nurse', 'Patient', 'admin', 'superadmin')")
    public ResponseEntity<List<CalendarEventDto>> getAppointmentsForCalendar(
            Authentication authentication) {
        
        String hospitalId = JwtContextHolder.getHospitalId();
        String userId = JwtContextHolder.getUserId();
        // Best effort to find highest role or primary role to filter. 
        // In JWT context Holder, finding role logic isn't natively bound to string without checking auth.
        // We will just extract the primary role string from Authentication
        String primaryRole = "";
        if (authentication != null && authentication.getAuthorities() != null) {
            primaryRole = authentication.getAuthorities().stream()
                            .map(r -> r.getAuthority().replace("ROLE_", ""))
                            .findFirst().orElse("");
        }
                            
        return ResponseEntity.ok(
                appointmentService.getAppointmentsForCalendar(hospitalId, primaryRole, userId));
    }

    // ─── GET /api/appointments/{id} ───────────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('Receptionist', 'Doctor', 'Nurse', 'admin', 'superadmin')")
    public ResponseEntity<AppointmentResponseDto> getAppointment(
            @PathVariable Integer id,
            Authentication authentication) {

        return ResponseEntity.ok(
                appointmentService.getAppointmentById(id, JwtContextHolder.getHospitalId()));
    }

    // ─── PATCH /api/appointments/{id}/status ─────────────────────────────────

    /**
     * Update appointment status only.
     * e.g. {"status": "Confirmed"} or {"status": "Treated"}
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('Receptionist', 'Doctor', 'admin', 'superadmin')")
    public ResponseEntity<AppointmentResponseDto> updateStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        String status     = body.get("status");
        String hospitalId = JwtContextHolder.getHospitalId();
        return ResponseEntity.ok(appointmentService.updateStatus(id, status, hospitalId));
    }

    // ─── PUT /api/appointments/{id} ───────────────────────────────────────────

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('Receptionist', 'Doctor', 'admin', 'superadmin')")
    public ResponseEntity<AppointmentResponseDto> updateAppointment(
            @PathVariable Integer id,
            @Valid @RequestBody AppointmentRequestDto dto,
            Authentication authentication) {

        return ResponseEntity.ok(
                appointmentService.updateAppointment(id, dto, JwtContextHolder.getHospitalId()));
    }

    // ─── DELETE /api/appointments/{id} ────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<Void> deleteAppointment(
            @PathVariable Integer id,
            Authentication authentication) {

        appointmentService.deleteAppointment(id, JwtContextHolder.getHospitalId());
        return ResponseEntity.noContent().build();
    }

    // ─── GET /api/appointments/requested ─────────────────────────────────────

    /**
     * List appointments submitted through the legacy patient-initiated request workflow.
     * These are appointments with a non-empty `request` column in the DB.
     */
    @GetMapping("/requested")
    @PreAuthorize("hasAnyAuthority('Receptionist', 'Doctor', 'admin', 'superadmin')")
    public ResponseEntity<Page<AppointmentResponseDto>> listRequested(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        String hospitalId = JwtContextHolder.getHospitalId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return ResponseEntity.ok(appointmentService.listRequested(hospitalId, pageable));
    }

    // ─── POST /api/appointments/{id}/confirm ─────────────────────────────────

    /**
     * Confirm a patient-requested appointment.
     * Sets status = 'Confirmed'. Mirrors legacy PHP confirm-request flow.
     */
    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyAuthority('Receptionist', 'Doctor', 'admin', 'superadmin')")
    public ResponseEntity<AppointmentResponseDto> confirmAppointment(
            @PathVariable Integer id,
            Authentication authentication) {

        return ResponseEntity.ok(
                appointmentService.confirmAppointment(id, JwtContextHolder.getHospitalId()));
    }

    // ─── POST /api/appointments/{id}/decline ─────────────────────────────────

    /**
     * Decline a patient-requested appointment.
     * Sets status = 'Declined'. Mirrors legacy PHP decline-request flow.
     */
    @PostMapping("/{id}/decline")
    @PreAuthorize("hasAnyAuthority('Receptionist', 'Doctor', 'admin', 'superadmin')")
    public ResponseEntity<AppointmentResponseDto> declineAppointment(
            @PathVariable Integer id,
            Authentication authentication) {

        return ResponseEntity.ok(
                appointmentService.declineAppointment(id, JwtContextHolder.getHospitalId()));
    }
}
