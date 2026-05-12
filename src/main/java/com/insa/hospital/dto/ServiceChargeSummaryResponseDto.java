package com.insa.hospital.dto;

import java.util.List;

public record ServiceChargeSummaryResponseDto(
        Totals totals,
        List<ServiceBreakdown> byService,
        List<GroupBreakdown> byRole,
        List<GroupBreakdown> byRevenueTarget,
        List<PatientBreakdown> byPatient,
        List<GroupBreakdown> byStatus
) {
    public record Totals(
            long chargeCount,
            long itemQuantity,
            String totalBilled,
            String totalCollected,
            String totalOutstanding
    ) {
    }

    public record ServiceBreakdown(
            Integer serviceCatalogId,
            String serviceName,
            String serviceGroup,
            String serviceRole,
            String revenueTarget,
            long chargeCount,
            long itemQuantity,
            String totalBilled,
            String totalCollected,
            String totalOutstanding
    ) {
    }

    public record GroupBreakdown(
            String key,
            long chargeCount,
            long itemQuantity,
            String totalBilled,
            String totalCollected,
            String totalOutstanding
    ) {
    }

    public record PatientBreakdown(
            String patientId,
            String patientName,
            long chargeCount,
            long itemQuantity,
            String totalBilled,
            String totalCollected,
            String totalOutstanding
    ) {
    }
}
