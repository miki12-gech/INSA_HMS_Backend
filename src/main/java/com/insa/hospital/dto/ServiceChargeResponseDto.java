package com.insa.hospital.dto;

import com.insa.hospital.entity.ServiceCharge;

public record ServiceChargeResponseDto(
        Long id,
        String hospitalId,
        String visitId,
        String patientId,
        String patientName,
        Integer serviceCatalogId,
        String serviceName,
        String serviceGroup,
        String serviceRole,
        String revenueTarget,
        String responsibleDoctorId,
        String responsibleDoctorName,
        String orderingStaffId,
        String orderingStaffName,
        String orderingStaffRole,
        String unitPrice,
        Integer quantity,
        String totalAmount,
        String paidAmount,
        String status,
        String notes,
        String chargedAt,
        String chargedDate
) {
    public static ServiceChargeResponseDto from(ServiceCharge charge) {
        return new ServiceChargeResponseDto(
                charge.getId(),
                charge.getHospitalId(),
                charge.getVisitId(),
                charge.getPatientId(),
                charge.getPatientName(),
                charge.getServiceCatalogId(),
                charge.getServiceName(),
                charge.getServiceGroup(),
                charge.getServiceRole(),
                charge.getRevenueTarget(),
                charge.getResponsibleDoctorId(),
                charge.getResponsibleDoctorName(),
                charge.getOrderingStaffId(),
                charge.getOrderingStaffName(),
                charge.getOrderingStaffRole(),
                charge.getUnitPrice(),
                charge.getQuantity(),
                charge.getTotalAmount(),
                charge.getPaidAmount(),
                charge.getStatus(),
                charge.getNotes(),
                charge.getChargedAt(),
                charge.getChargedDate()
        );
    }
}
