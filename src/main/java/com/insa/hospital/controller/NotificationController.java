package com.insa.hospital.controller;

import com.insa.hospital.dto.VisibleNotificationDto;
import com.insa.hospital.entity.Notice;
import com.insa.hospital.security.JwtContextHolder;
import com.insa.hospital.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller — Notice Board & Email Notification endpoints.
 *
 * Base URL: /api/notifications
 *
 * Endpoints:
 *   GET    /api/notifications            → Paginated notice list
 *   GET    /api/notifications?type=patient → Filter by audience type
 *   POST   /api/notifications            → Post new notice
 *   PUT    /api/notifications/{id}       → Update notice
 *   DELETE /api/notifications/{id}       → Delete notice
 *   POST   /api/notifications/email      → Send email (mock — logs to console)
 *   GET    /api/notifications/emails     → Sent email log
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** GET /api/notifications?type=patient&page=0&size=20 */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<?> listNotices(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        String hospitalId = JwtContextHolder.getHospitalId();
        if (type != null && !type.isBlank()) {
            List<Notice> notices = notificationService.listNoticesByType(type, hospitalId);
            return ResponseEntity.ok(notices);
        }
        Page<Notice> notices = notificationService.listNotices(hospitalId,
                PageRequest.of(page, size, Sort.by("id").descending()));
        return ResponseEntity.ok(notices);
    }

    /** GET /api/notifications/visible -> notices for the current role (Receptionist excluded) */
    @GetMapping("/visible")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<VisibleNotificationDto>> listVisibleNotices() {
        return ResponseEntity.ok(
                notificationService.listVisibleNotices(
                        JwtContextHolder.getHospitalId(),
                        JwtContextHolder.getRole()
                )
        );
    }

    /** POST /api/notifications — Post new notice */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<Notice> createNotice(@RequestBody Notice notice) {
        return ResponseEntity.ok(
            notificationService.createNotice(notice, JwtContextHolder.getHospitalId()));
    }

    /** PUT /api/notifications/{id} */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<Notice> updateNotice(@PathVariable Integer id, @RequestBody Notice body) {
        return ResponseEntity.ok(
            notificationService.updateNotice(id, body, JwtContextHolder.getHospitalId()));
    }

    /** DELETE /api/notifications/{id} */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<Void> deleteNotice(@PathVariable Integer id) {
        notificationService.deleteNotice(id, JwtContextHolder.getHospitalId());
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/notifications/email
     * Triggers the mock email sender (logs to console, saves to email table).
     * Body: { "to": "patient@example.com", "subject": "Appointment Reminder", "body": "..." }
     */
    @PostMapping("/email")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin', 'Doctor', 'Receptionist')")
    public ResponseEntity<Map<String, String>> sendEmail(@RequestBody Map<String, String> req) {
        notificationService.sendEmail(
            req.get("to"),
            req.get("subject"),
            req.get("body"),
            JwtContextHolder.getUserId(),
            JwtContextHolder.getHospitalId()
        );
        return ResponseEntity.ok(Map.of("status", "sent", "note", "Mock mode: logged to console only"));
    }

    /** GET /api/notifications/emails — Sent email log */
    @GetMapping("/emails")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<?> listSentEmails(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
            notificationService.listSentEmails(JwtContextHolder.getHospitalId(),
                    PageRequest.of(page, size, Sort.by("id").descending())));
    }
}
