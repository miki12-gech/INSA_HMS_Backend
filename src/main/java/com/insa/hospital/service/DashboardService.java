package com.insa.hospital.service;

import com.insa.hospital.dto.DashboardSummaryResponseDto;
import com.insa.hospital.dto.DashboardSummaryResponseDto.RecentPatientDto;
import com.insa.hospital.dto.DashboardSummaryResponseDto.RecentPaymentDto;
import com.insa.hospital.entity.Patient;
import com.insa.hospital.entity.Payment;
import com.insa.hospital.repository.AppointmentRepository;
import com.insa.hospital.repository.LabRepository;
import com.insa.hospital.repository.PatientRepository;
import com.insa.hospital.repository.PatientVisitRepository;
import com.insa.hospital.repository.PaymentRepository;
import com.insa.hospital.dto.PatientVisitResponseDto;
import com.insa.hospital.entity.PatientVisit;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import com.insa.hospital.service.TenantAccessService;
import java.util.List;

/**
 * Dashboard Service — Aggregates data from multiple repositories for
 * the admin/doctor summary view.
 *
 * ══════════════════════════════════════════════════════════════
 *  DATE HANDLING — "Today" Epoch Range
 * ══════════════════════════════════════════════════════════════
 *  Legacy tables store dates as Unix epoch strings (varchar).
 *  "Today" is defined as epoch range:
 *    startEpoch = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toEpochSecond()
 *    endEpoch   = startEpoch + 86400 - 1   (last second of today)
 *
 *  These longs are passed to native SQL queries that use:
 *    CAST(date AS UNSIGNED) BETWEEN :startEpoch AND :endEpoch
 *
 *  This mirrors how the legacy PHP controller computes its daily
 *  counts (mktime(0,0,0) to mktime(23,59,59)).
 * ══════════════════════════════════════════════════════════════
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final long SECONDS_IN_DAY = 86_400L;

    private final PatientRepository     patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final PaymentRepository     paymentRepository;
    private final LabRepository         labRepository;
    private final PatientVisitRepository patientVisitRepository;
    private final TenantAccessService tenantAccessService;

    @Autowired
    public DashboardService(PatientRepository patientRepository,
                            AppointmentRepository appointmentRepository,
                            PaymentRepository paymentRepository,
                            LabRepository labRepository,
                            PatientVisitRepository patientVisitRepository,
                            TenantAccessService tenantAccessService) {
        this.patientRepository      = patientRepository;
        this.appointmentRepository  = appointmentRepository;
        this.paymentRepository      = paymentRepository;
        this.labRepository          = labRepository;
        this.patientVisitRepository = patientVisitRepository;
        this.tenantAccessService    = tenantAccessService;
    }

    /**
     * Builds the full dashboard summary for the given hospitalId.
     *
     * All "today" queries use a CAST(date AS UNSIGNED) BETWEEN range
     * computed from UTC midnight.
     */
    public DashboardSummaryResponseDto getDashboardSummary(String hospitalId) {

        // ── Compute today's epoch window ───────────────────────────────────────
        long startEpoch = todayStartEpoch();
        long endEpoch   = startEpoch + SECONDS_IN_DAY - 1;

        // ── Patient metrics ────────────────────────────────────────────────────
        long totalPatients      = patientRepository.countByHospitalId(hospitalId);
        long todayNewPatients   = patientRepository.countTodayRegistrations(
                                        hospitalId, startEpoch, endEpoch);

        // ── Appointment metrics ────────────────────────────────────────────────
        long totalAppointments   = appointmentRepository.countByHospitalId(hospitalId);
        long todayAppointments   = appointmentRepository.countTodayAppointments(
                                        hospitalId, startEpoch, endEpoch);
        long pendingAppointments = appointmentRepository.countByStatusAndHospitalId(
                                        "Pending Confirmation", hospitalId);

        // ── Revenue metrics ────────────────────────────────────────────────────
        // COALESCE in native SQL guarantees non-null, but we still default to 0.0
        Double rawRevenue = paymentRepository.sumTodayRevenue(hospitalId, startEpoch, endEpoch);
        double todayRevenue = (rawRevenue != null) ? rawRevenue : 0.0;

        long unpaidInvoices = paymentRepository.countByStatusAndHospitalId("unpaid", hospitalId);

        // ── Lab metrics ────────────────────────────────────────────────────────
        long pendingLabRequests = labRepository.findByStatusAndHospitalId(
                "Pending Confirmation", hospitalId,
                org.springframework.data.domain.PageRequest.of(0, 1))
                .getTotalElements();

        // ── Recent activity feeds ──────────────────────────────────────────────
        List<RecentPatientDto> recentPatients = patientRepository
                .findTop5RecentByHospitalId(hospitalId)
                .stream()
                .map(this::toRecentPatientDto)
                .toList();

        List<RecentPaymentDto> recentPayments = paymentRepository
                .findTop5RecentByHospitalId(hospitalId)
                .stream()
                .map(this::toRecentPaymentDto)
                .toList();

        List<Patient> todayRegisteredList = patientRepository.findTodayRegistrations(hospitalId, startEpoch, endEpoch);
        List<RecentPatientDto> todayRegisteredPatients = todayRegisteredList.stream()
                .map(this::toRecentPatientDto)
                .toList();

        String effectiveHospitalId = tenantAccessService.normalizeHospitalScopeId(hospitalId);
        List<PatientVisit> rawVisits = patientVisitRepository.findTodayVisits(effectiveHospitalId, startEpoch, endEpoch);
        List<Integer> patientIds = rawVisits.stream()
                .map(PatientVisit::getPatientId)
                .map(idStr -> {
                    try { return Integer.parseInt(idStr); } catch (Exception e) { return null; }
                })
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Integer, Patient> patientMap = new HashMap<>();
        if (!patientIds.isEmpty()) {
            patientRepository.findAllById(patientIds).forEach(p -> patientMap.put(p.getId(), p));
        }

        List<PatientVisitResponseDto> todayVisits = rawVisits.stream()
                .map(v -> {
                    Integer pId = null;
                    try { pId = Integer.parseInt(v.getPatientId()); } catch (Exception e) {}
                    return PatientVisitResponseDto.from(v, pId != null ? patientMap.get(pId) : null);
                })
                .toList();

        return new DashboardSummaryResponseDto(
            totalPatients,
            todayNewPatients,
            totalAppointments,
            todayAppointments,
            pendingAppointments,
            todayRevenue,
            unpaidInvoices,
            pendingLabRequests,
            recentPatients,
            recentPayments,
            System.currentTimeMillis() / 1000L,
            todayRegisteredPatients,
            todayVisits
        );
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────

    /**
     * Returns the Unix epoch second for UTC midnight of the current calendar day.
     * Matches the legacy PHP mktime(0,0,0) approach.
     */
    private long todayStartEpoch() {
        return LocalDate.now(ZoneOffset.UTC)
                .atStartOfDay(ZoneOffset.UTC)
                .toEpochSecond();
    }

    private RecentPatientDto toRecentPatientDto(Patient p) {
        return new RecentPatientDto(
            p.getId(),
            p.getPatientId(),
            p.getName(),
            p.getPhone(),
            p.getSex(),
            p.getRegistrationTime()
        );
    }

    private RecentPaymentDto toRecentPaymentDto(Payment p) {
        return new RecentPaymentDto(
            p.getId(),
            p.getPatientName(),
            p.getGrossTotal(),
            p.getStatus(),
            p.getDepositType(),
            p.getDateString()
        );
    }
}
