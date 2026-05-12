package com.insa.hospital.service;

import com.insa.hospital.dto.VisibleNotificationDto;
import com.insa.hospital.entity.Email;
import com.insa.hospital.entity.Notice;
import com.insa.hospital.entity.ReferralRequest;
import com.insa.hospital.entity.ReferralRequest.ReferralStatus;
import com.insa.hospital.entity.ReferralRequest.RequestedByRole;
import com.insa.hospital.repository.EmailRepository;
import com.insa.hospital.repository.NoticeRepository;
import com.insa.hospital.repository.ReferralRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Notification Service — handles Notice board and Email logging.
 *
 * ══════════════════════════════════════════════════════════════
 *  Email Sending — MOCK IMPLEMENTATION
 * ══════════════════════════════════════════════════════════════
 *  sendEmail() currently ONLY logs to console.
 *  No SMTP dependency is needed at this stage, preventing startup
 *  failures if no mail server is configured.
 *
 *  To enable real sending later:
 *    1. Add spring-boot-starter-mail to pom.xml
 *    2. Configure spring.mail.* in application.yml
 *    3. Replace the log.info() block with JavaMailSender calls
 *
 *  The Email record IS still persisted to `email` table as a log,
 *  even in mock mode.
 * ══════════════════════════════════════════════════════════════
 */
@Service
@Transactional
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final Set<String> STAFF_ROLES = Set.of(
            "admin",
            "superadmin",
            "doctor",
            "nurse",
            "accountant",
            "laboratorist",
            "pharmacist",
            "receptionist"
    );
    private static final long MEDICAL_REQUEST_NOTIFICATION_ID_OFFSET = 1_000_000_000L;
    private static final long REFERRAL_NOTIFICATION_ID_OFFSET = 2_000_000_000L;

    private final NoticeRepository noticeRepository;
    private final EmailRepository emailRepository;
    private final ReferralRequestRepository referralRequestRepository;

    @Autowired
    public NotificationService(NoticeRepository noticeRepository,
                               EmailRepository emailRepository,
                               ReferralRequestRepository referralRequestRepository) {
        this.noticeRepository = noticeRepository;
        this.emailRepository = emailRepository;
        this.referralRequestRepository = referralRequestRepository;
    }

    // ─── Notice Board ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<Notice> listNotices(String hospitalId, Pageable pageable) {
        return noticeRepository.findByHospitalId(hospitalId, pageable);
    }

    @Transactional(readOnly = true)
    public List<Notice> listNoticesByType(String type, String hospitalId) {
        return noticeRepository.findByTypeAndHospitalId(type, hospitalId);
    }

    @Transactional(readOnly = true)
    public List<VisibleNotificationDto> listVisibleNotices(String hospitalId, String role) {
        Set<String> audiences = resolveVisibleAudiences(role);
        if (audiences.isEmpty()) {
            return List.of();
        }

        String normalizedRole = normalizeAudience(role);
        List<VisibleNotificationDto> items = new ArrayList<>();

        noticeRepository.findByHospitalIdOrderByIdDesc(hospitalId).stream()
                .filter(notice -> audiences.contains(normalizeAudience(notice.getType())))
                .map(VisibleNotificationDto::fromNotice)
                .forEach(items::add);

        if (canReviewReferralQueues(normalizedRole)) {
            items.addAll(buildPendingReferralNotifications(hospitalId));
        }

        return items.stream()
                .sorted(Comparator.comparingLong(
                        (VisibleNotificationDto item) -> toEpochMillis(item.date()))
                        .reversed())
                .toList();
    }

    @Transactional
    public Notice createNotice(Notice notice, String hospitalId) {
        notice.setHospitalId(hospitalId);
        notice.setDate(String.valueOf(System.currentTimeMillis() / 1000L));
        notice.setType(normalizeAudience(notice.getType()));
        return noticeRepository.save(notice);
    }

    @Transactional
    public Notice updateNotice(Integer id, Notice body, String hospitalId) {
        Notice existing = noticeRepository.findById(id)
                .filter(n -> hospitalId.equals(n.getHospitalId()))
                .orElseThrow(() -> new com.insa.hospital.exception.ResourceNotFoundException("Notice", "id", id));
        existing.setTitle(body.getTitle());
        existing.setDescription(body.getDescription());
        existing.setType(normalizeAudience(body.getType()));
        return noticeRepository.save(existing);
    }

    @Transactional
    public void deleteNotice(Integer id, String hospitalId) {
        Notice n = noticeRepository.findById(id)
                .filter(notice -> hospitalId.equals(notice.getHospitalId()))
                .orElseThrow(() -> new com.insa.hospital.exception.ResourceNotFoundException("Notice", "id", id));
        noticeRepository.delete(n);
    }

    // ─── Email (Log + Mock Send) ───────────────────────────────────────────────

    /**
     * MOCK EMAIL SENDER.
     * Logs the email details to console and persists a record to the `email`
     * table. Does NOT connect to any SMTP server.
     *
     * Replace the log.info() block with JavaMailSender calls when ready.
     */
    public void sendEmail(String to, String subject, String body,
                          String senderUserId, String hospitalId) {
        // ── MOCK: Log to console ──────────────────────────────────────────────
        log.info("═══════════════════════════════════════════");
        log.info("  [MOCK EMAIL] Sending email to: {}", to);
        log.info("  Subject:  {}", subject);
        log.info("  Body:     {}", body);
        log.info("  Hospital: {}", hospitalId);
        log.info("═══════════════════════════════════════════");
        // ── END MOCK ──────────────────────────────────────────────────────────

        // Persist email log to `email` table regardless of mock/real mode
        Email emailLog = new Email();
        emailLog.setReciepient(to); // legacy typo preserved
        emailLog.setSubject(subject);
        emailLog.setMessage(body);
        emailLog.setUser(senderUserId);
        emailLog.setDate(String.valueOf(System.currentTimeMillis() / 1000L));
        emailLog.setHospitalId(hospitalId);
        emailRepository.save(emailLog);
    }

    @Transactional(readOnly = true)
    public Page<Email> listSentEmails(String hospitalId, Pageable pageable) {
        return emailRepository.findByHospitalId(hospitalId, pageable);
    }

    private Set<String> resolveVisibleAudiences(String role) {
        String normalizedRole = normalizeAudience(role);
        if ("receptionist".equals(normalizedRole)) {
            return Set.of();
        }

        LinkedHashSet<String> audiences = new LinkedHashSet<>();
        audiences.add("all");

        if ("patient".equals(normalizedRole)) {
            audiences.add("patient");
            return audiences;
        }

        if (STAFF_ROLES.contains(normalizedRole)) {
            audiences.add("staff");
            audiences.add("all_staff");
        }

        if (!normalizedRole.isBlank()) {
            audiences.add(normalizedRole);
        }

        return audiences;
    }

    private boolean canReviewReferralQueues(String normalizedRole) {
        return "admin".equals(normalizedRole) || "superadmin".equals(normalizedRole);
    }

    private List<VisibleNotificationDto> buildPendingReferralNotifications(String hospitalId) {
        List<ReferralRequest> pendingRequests;
        if (StringUtils.hasText(hospitalId)) {
            pendingRequests = referralRequestRepository
                    .findByStatusAndHospitalIdOrderByIdDesc(ReferralStatus.PENDING, hospitalId);
        } else {
            pendingRequests = referralRequestRepository.findByStatusOrderByIdDesc(ReferralStatus.PENDING);
        }

        return pendingRequests.stream()
                .map(this::toPendingReferralNotification)
                .toList();
    }

    private VisibleNotificationDto toPendingReferralNotification(ReferralRequest referral) {
        boolean medicalRequest = referral.getRequestedByRole() == RequestedByRole.EMPLOYEE;
        String patientId = HtmlUtils.htmlEscape(
                StringUtils.hasText(referral.getPatientId()) ? referral.getPatientId().trim() : "Unknown");
        String destinationHospital = HtmlUtils.htmlEscape(
                StringUtils.hasText(referral.getDestinationHospital())
                        ? referral.getDestinationHospital().trim()
                        : "destination hospital");

        String title = medicalRequest
                ? "New medical request - " + patientId
                : "New referral - " + patientId;

        String description = medicalRequest
                ? "<p>Pending employee medical request to " + destinationHospital
                + " for ID " + patientId + ".</p>"
                : "<p>Pending doctor referral to " + destinationHospital
                + " for patient ID " + patientId + ".</p>";

        long offset = medicalRequest
                ? MEDICAL_REQUEST_NOTIFICATION_ID_OFFSET
                : REFERRAL_NOTIFICATION_ID_OFFSET;
        String route = medicalRequest
                ? "/dashboard/medical-requests?focus=" + referral.getId()
                : "/dashboard/referrals?focus=" + referral.getId();

        return new VisibleNotificationDto(
                offset + referral.getId(),
                title,
                description,
                medicalRequest ? "medical_request" : "referral",
                StringUtils.hasText(referral.getCreatedAt())
                        ? referral.getCreatedAt()
                        : String.valueOf(System.currentTimeMillis()),
                route
        );
    }

    private String normalizeAudience(String value) {
        if (value == null || value.isBlank()) {
            return "all_staff";
        }

        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
    }

    private long toEpochMillis(String value) {
        if (!StringUtils.hasText(value)) {
            return Long.MIN_VALUE;
        }

        String trimmed = value.trim();
        try {
            long numeric = Long.parseLong(trimmed);
            return trimmed.length() <= 10 ? numeric * 1000L : numeric;
        } catch (NumberFormatException ignored) {
            // Fall through to ISO parsing.
        }

        try {
            return Instant.parse(trimmed).toEpochMilli();
        } catch (DateTimeParseException ignored) {
            // Fall through to local timestamp parsing.
        }

        try {
            return LocalDateTime.parse(trimmed)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
        } catch (DateTimeParseException ignored) {
            // Fall through to date-only parsing.
        }

        try {
            return LocalDate.parse(trimmed)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
        } catch (DateTimeParseException ignored) {
            return Long.MIN_VALUE;
        }
    }
}
