package com.insa.hospital.service;

import com.insa.hospital.dto.ServiceChargeBulkCreateRequestDto;
import com.insa.hospital.dto.ServiceChargeResponseDto;
import com.insa.hospital.dto.ServiceChargeSummaryResponseDto;
import com.insa.hospital.entity.Doctor;
import com.insa.hospital.entity.Patient;
import com.insa.hospital.entity.PaymentCategory;
import com.insa.hospital.entity.ServiceCharge;
import com.insa.hospital.exception.ResourceNotFoundException;
import com.insa.hospital.repository.DoctorRepository;
import com.insa.hospital.repository.PatientRepository;
import com.insa.hospital.repository.PaymentCategoryRepository;
import com.insa.hospital.repository.ServiceChargeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ServiceChargeService {

    private final ServiceChargeRepository serviceChargeRepository;
    private final PaymentCategoryRepository paymentCategoryRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public List<ServiceChargeResponseDto> createCharges(
            ServiceChargeBulkCreateRequestDto dto,
            String hospitalId,
            String orderingStaffId,
            String orderingStaffName,
            String orderingStaffRole
    ) {
        String nowEpoch = epochNow();
        String chargeDate = LocalDate.now().toString();
        String patientName = resolvePatientName(dto.patientId(), dto.patientName());
        String doctorName = resolveDoctorName(dto.responsibleDoctorId(), dto.responsibleDoctorName());

        List<ServiceChargeResponseDto> created = new ArrayList<>();
        for (ServiceChargeBulkCreateRequestDto.ServiceChargeItemDto item : dto.items()) {
            PaymentCategory category = paymentCategoryRepository.findById(item.serviceCatalogId())
                    .filter(entry -> hospitalId.equals(entry.getHospitalId()))
                    .orElseThrow(() -> new ResourceNotFoundException("PaymentCategory", "id", item.serviceCatalogId()));

            int quantity = item.quantity() != null ? item.quantity() : 1;
            double unitPrice = parseAmount(StringUtils.hasText(item.unitPrice()) ? item.unitPrice() : category.getCPrice());
            double totalAmount = unitPrice * quantity;

            ServiceCharge charge = new ServiceCharge();
            charge.setHospitalId(hospitalId);
            charge.setVisitId(trimToNull(dto.visitId()));
            charge.setPatientId(dto.patientId().trim());
            charge.setPatientName(patientName);
            charge.setServiceCatalogId(category.getId());
            charge.setServiceName(category.getCategory());
            charge.setServiceGroup(defaultIfBlank(category.getServiceGroup(), category.getType(), "General"));
            charge.setServiceRole(defaultIfBlank(category.getServiceRole(), "Hospital"));
            charge.setRevenueTarget(defaultIfBlank(category.getRevenueTarget(), "Hospital Revenue"));
            charge.setResponsibleDoctorId(trimToNull(dto.responsibleDoctorId()));
            charge.setResponsibleDoctorName(doctorName);
            charge.setOrderingStaffId(trimToNull(orderingStaffId));
            charge.setOrderingStaffName(trimToNull(orderingStaffName));
            charge.setOrderingStaffRole(trimToNull(orderingStaffRole));
            charge.setUnitPrice(formatMoney(unitPrice));
            charge.setQuantity(quantity);
            charge.setTotalAmount(formatMoney(totalAmount));
            charge.setPaidAmount("0");
            charge.setStatus("PENDING");
            charge.setNotes(trimToNull(dto.notes()));
            charge.setChargedAt(nowEpoch);
            charge.setChargedDate(chargeDate);
            charge.setCreatedAt(nowEpoch);
            charge.setUpdatedAt(nowEpoch);

            created.add(ServiceChargeResponseDto.from(serviceChargeRepository.save(charge)));
        }
        return created;
    }

    @Transactional(readOnly = true)
    public List<ServiceChargeResponseDto> listCharges(
            String hospitalId,
            String patientId,
            String visitId,
            String status,
            String dateFrom,
            String dateTo
    ) {
        List<ServiceCharge> base;
        if (StringUtils.hasText(visitId)) {
            base = serviceChargeRepository.findByHospitalIdAndVisitIdOrderByIdDesc(hospitalId, visitId.trim());
        } else if (StringUtils.hasText(patientId)) {
            base = serviceChargeRepository.findByHospitalIdAndPatientIdOrderByIdDesc(hospitalId, patientId.trim());
        } else {
            base = serviceChargeRepository.findByHospitalIdOrderByIdDesc(hospitalId);
        }

        return base.stream()
                .filter(charge -> !StringUtils.hasText(status) || status.equalsIgnoreCase(charge.getStatus()))
                .filter(charge -> isWithinDate(charge.getChargedDate(), dateFrom, dateTo))
                .map(ServiceChargeResponseDto::from)
                .toList();
    }

    public ServiceChargeResponseDto collectPayment(Long id, String paidAmount, String hospitalId) {
        ServiceCharge charge = getScopedCharge(id, hospitalId);
        double collected = Math.max(0, parseAmount(paidAmount));
        charge.setPaidAmount(formatMoney(collected));
        charge.setStatus(resolveStatus(collected, parseAmount(charge.getTotalAmount()), charge.getStatus()));
        charge.setUpdatedAt(epochNow());
        return ServiceChargeResponseDto.from(serviceChargeRepository.save(charge));
    }

    public ServiceChargeResponseDto updateStatus(Long id, String status, String hospitalId) {
        ServiceCharge charge = getScopedCharge(id, hospitalId);
        charge.setStatus(StringUtils.hasText(status) ? status.trim().toUpperCase() : charge.getStatus());
        charge.setUpdatedAt(epochNow());
        return ServiceChargeResponseDto.from(serviceChargeRepository.save(charge));
    }

    @Transactional(readOnly = true)
    public ServiceChargeSummaryResponseDto buildSummary(String hospitalId, String dateFrom, String dateTo) {
        List<ServiceCharge> charges = serviceChargeRepository.findByHospitalIdOrderByIdDesc(hospitalId).stream()
                .filter(charge -> isWithinDate(charge.getChargedDate(), dateFrom, dateTo))
                .toList();

        long chargeCount = charges.size();
        long itemQuantity = charges.stream().mapToLong(charge -> charge.getQuantity() == null ? 0 : charge.getQuantity()).sum();
        double totalBilled = charges.stream().mapToDouble(charge -> parseAmount(charge.getTotalAmount())).sum();
        double totalCollected = charges.stream().mapToDouble(charge -> parseAmount(charge.getPaidAmount())).sum();
        double totalOutstanding = Math.max(0, totalBilled - totalCollected);

        return new ServiceChargeSummaryResponseDto(
                new ServiceChargeSummaryResponseDto.Totals(
                        chargeCount,
                        itemQuantity,
                        formatMoney(totalBilled),
                        formatMoney(totalCollected),
                        formatMoney(totalOutstanding)
                ),
                buildServiceBreakdown(charges),
                buildGroupBreakdown(charges, Grouping.SERVICE_ROLE),
                buildGroupBreakdown(charges, Grouping.REVENUE_TARGET),
                buildPatientBreakdown(charges),
                buildGroupBreakdown(charges, Grouping.STATUS)
        );
    }

    private List<ServiceChargeSummaryResponseDto.ServiceBreakdown> buildServiceBreakdown(List<ServiceCharge> charges) {
        Map<String, SummaryAccumulator> grouped = new LinkedHashMap<>();
        for (ServiceCharge charge : charges) {
            String key = String.format("%s|%s", charge.getServiceCatalogId(), defaultIfBlank(charge.getServiceName(), "Unnamed service"));
            grouped.computeIfAbsent(key, ignored -> new SummaryAccumulator())
                    .accumulate(charge);
        }

        return grouped.entrySet().stream()
                .map(entry -> {
                    String[] parts = entry.getKey().split("\\|", 2);
                    SummaryAccumulator acc = entry.getValue();
                    ServiceCharge sample = acc.sample;
                    Integer serviceCatalogId = null;
                    try {
                        serviceCatalogId = parts[0].equals("null") ? null : Integer.valueOf(parts[0]);
                    } catch (NumberFormatException ignored) {
                    }
                    return new ServiceChargeSummaryResponseDto.ServiceBreakdown(
                            serviceCatalogId,
                            sample.getServiceName(),
                            sample.getServiceGroup(),
                            sample.getServiceRole(),
                            sample.getRevenueTarget(),
                            acc.chargeCount,
                            acc.itemQuantity,
                            formatMoney(acc.totalBilled),
                            formatMoney(acc.totalCollected),
                            formatMoney(acc.totalBilled - acc.totalCollected)
                    );
                })
                .sorted(Comparator.comparingDouble(
                        (ServiceChargeSummaryResponseDto.ServiceBreakdown row) -> parseAmount(row.totalBilled()))
                        .reversed())
                .toList();
    }

    private List<ServiceChargeSummaryResponseDto.GroupBreakdown> buildGroupBreakdown(List<ServiceCharge> charges, Grouping grouping) {
        Map<String, SummaryAccumulator> grouped = new LinkedHashMap<>();
        for (ServiceCharge charge : charges) {
            String key = switch (grouping) {
                case SERVICE_ROLE -> defaultIfBlank(charge.getServiceRole(), "Unassigned");
                case REVENUE_TARGET -> defaultIfBlank(charge.getRevenueTarget(), "Unassigned");
                case STATUS -> defaultIfBlank(charge.getStatus(), "UNKNOWN");
            };
            grouped.computeIfAbsent(key, ignored -> new SummaryAccumulator())
                    .accumulate(charge);
        }

        return grouped.entrySet().stream()
                .map(entry -> new ServiceChargeSummaryResponseDto.GroupBreakdown(
                        entry.getKey(),
                        entry.getValue().chargeCount,
                        entry.getValue().itemQuantity,
                        formatMoney(entry.getValue().totalBilled),
                        formatMoney(entry.getValue().totalCollected),
                        formatMoney(entry.getValue().totalBilled - entry.getValue().totalCollected)
                ))
                .sorted(Comparator.comparingDouble(
                        (ServiceChargeSummaryResponseDto.GroupBreakdown row) -> parseAmount(row.totalBilled()))
                        .reversed())
                .toList();
    }

    private List<ServiceChargeSummaryResponseDto.PatientBreakdown> buildPatientBreakdown(List<ServiceCharge> charges) {
        Map<String, SummaryAccumulator> grouped = new LinkedHashMap<>();
        for (ServiceCharge charge : charges) {
            String key = String.format("%s|%s", charge.getPatientId(), defaultIfBlank(charge.getPatientName(), "Unknown patient"));
            grouped.computeIfAbsent(key, ignored -> new SummaryAccumulator())
                    .accumulate(charge);
        }

        return grouped.entrySet().stream()
                .map(entry -> {
                    String[] parts = entry.getKey().split("\\|", 2);
                    SummaryAccumulator acc = entry.getValue();
                    return new ServiceChargeSummaryResponseDto.PatientBreakdown(
                            parts[0],
                            parts.length > 1 ? parts[1] : "",
                            acc.chargeCount,
                            acc.itemQuantity,
                            formatMoney(acc.totalBilled),
                            formatMoney(acc.totalCollected),
                            formatMoney(acc.totalBilled - acc.totalCollected)
                    );
                })
                .sorted(Comparator.comparingDouble(
                        (ServiceChargeSummaryResponseDto.PatientBreakdown row) -> parseAmount(row.totalBilled()))
                        .reversed())
                .toList();
    }

    private boolean isWithinDate(String value, String dateFrom, String dateTo) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        if (StringUtils.hasText(dateFrom) && value.compareTo(dateFrom.trim()) < 0) {
            return false;
        }
        if (StringUtils.hasText(dateTo) && value.compareTo(dateTo.trim()) > 0) {
            return false;
        }
        return true;
    }

    private ServiceCharge getScopedCharge(Long id, String hospitalId) {
        return serviceChargeRepository.findById(id)
                .filter(charge -> hospitalId.equals(charge.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("ServiceCharge", "id", id));
    }

    private String resolvePatientName(String patientId, String fallback) {
        if (StringUtils.hasText(fallback)) {
            return fallback.trim();
        }
        if (!StringUtils.hasText(patientId)) {
            return "";
        }
        try {
            return patientRepository.findById(Integer.parseInt(patientId.trim()))
                    .map(Patient::getName)
                    .orElse("");
        } catch (NumberFormatException ignored) {
            return "";
        }
    }

    private String resolveDoctorName(String doctorId, String fallback) {
        if (StringUtils.hasText(fallback)) {
            return fallback.trim();
        }
        if (!StringUtils.hasText(doctorId)) {
            return "";
        }
        try {
            return doctorRepository.findById(Integer.parseInt(doctorId.trim()))
                    .map(Doctor::getName)
                    .orElse("");
        } catch (NumberFormatException ignored) {
            return "";
        }
    }

    private String resolveStatus(double collected, double total, String currentStatus) {
        if ("CANCELLED".equalsIgnoreCase(currentStatus)) {
            return "CANCELLED";
        }
        if (collected <= 0) {
            return "PENDING";
        }
        if (collected >= total) {
            return "PAID";
        }
        return "PARTIAL";
    }

    private double parseAmount(String value) {
        if (!StringUtils.hasText(value)) {
            return 0;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String formatMoney(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((long) value);
        }
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    private String epochNow() {
        return String.valueOf(System.currentTimeMillis() / 1000L);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String defaultIfBlank(String first, String second, String fallback) {
        if (StringUtils.hasText(first)) {
            return first.trim();
        }
        if (StringUtils.hasText(second)) {
            return second.trim();
        }
        return fallback;
    }

    private String defaultIfBlank(String first, String fallback) {
        return defaultIfBlank(first, null, fallback);
    }

    private enum Grouping {
        SERVICE_ROLE,
        REVENUE_TARGET,
        STATUS
    }

    private static final class SummaryAccumulator {
        private ServiceCharge sample;
        private long chargeCount;
        private long itemQuantity;
        private double totalBilled;
        private double totalCollected;

        private void accumulate(ServiceCharge charge) {
            if (sample == null) {
                sample = charge;
            }
            chargeCount++;
            itemQuantity += charge.getQuantity() == null ? 0 : charge.getQuantity();
            totalBilled += safeParse(charge.getTotalAmount());
            totalCollected += safeParse(charge.getPaidAmount());
        }

        private double safeParse(String value) {
            if (value == null || value.isBlank()) {
                return 0;
            }
            try {
                return Double.parseDouble(value.trim());
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
    }
}
