package com.insa.hospital.dto;

import java.util.List;

/**
 * Dashboard Summary Response DTO.
 *
 * Aggregates key metrics from the legacy tables for the admin/doctor
 * dashboard view. All numbers are for the requesting hospital only
 * (multi-tenant scoped).
 *
 * "Today" is computed using UTC epoch range in the service layer.
 */
public record DashboardSummaryResponseDto(

    // ── Patient Metrics ──────────────────────────────────────────────────────

    /** Total patients ever registered for this hospital. */
    long totalPatients,

    /** Patients registered today (epoch range). */
    long todayNewPatients,

    // ── Appointment Metrics ──────────────────────────────────────────────────

    /** Total appointments ever booked for this hospital. */
    long totalAppointments,

    /** Appointments booked/scheduled for today (epoch range). */
    long todayAppointments,

    /** Appointments with status = 'Pending Confirmation'. */
    long pendingAppointments,

    // ── Revenue Metrics ───────────────────────────────────────────────────────

    /**
     * Sum of gross_total for all payments created today.
     * 0.0 if no payments recorded today.
     * Formatted to 2 decimal places.
     */
    double todayRevenue,

    /** Count of invoices with status = 'unpaid'. */
    long unpaidInvoices,

    // ── Lab Metrics ───────────────────────────────────────────────────────────

    /** Lab orders pending: status = 'Pending Confirmation'. */
    long pendingLabRequests,

    // ── Recent Activity Feeds ─────────────────────────────────────────────────

    /** Last 5 recently registered patients (for activity feed). */
    List<RecentPatientDto> recentPatients,

    /** Last 5 recent payments (for revenue feed). */
    List<RecentPaymentDto> recentPayments,

    /** Server timestamp when this summary was generated (epoch seconds). */
    long generatedAt,

    /** All patients registered today */
    List<RecentPatientDto> todayRegisteredPatients,

    /** All visits started today */
    List<PatientVisitResponseDto> todayVisits

) {

    /** Mini patient summary for the recent activity feed. */
    public record RecentPatientDto(
        Integer id,
        String patientId,
        String name,
        String phone,
        String sex,
        String registrationTime
    ) {}

    /** Mini payment summary for the revenue activity feed. */
    public record RecentPaymentDto(
        Integer id,
        String patientName,
        String grossTotal,
        String status,
        String depositType,
        String dateString
    ) {}
}
