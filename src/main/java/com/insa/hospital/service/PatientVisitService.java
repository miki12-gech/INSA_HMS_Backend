package com.insa.hospital.service;

import com.insa.hospital.dto.PatientVisitResponseDto;
import com.insa.hospital.dto.PatientVisitStartRequestDto;
import com.insa.hospital.dto.PatientVisitStatusUpdateRequestDto;
import com.insa.hospital.entity.Patient;
import com.insa.hospital.entity.PatientVisit;
import com.insa.hospital.exception.BadRequestException;
import com.insa.hospital.exception.ResourceNotFoundException;
import com.insa.hospital.repository.PatientRepository;
import com.insa.hospital.repository.PatientVisitRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
@Transactional
public class PatientVisitService {

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "WAITING_TRIAGE",
            "WAITING_CONSULTATION",
            "IN_CONSULTATION",
            "PENDING_LAB",
            "LAB_COMPLETED",
            "PENDING_PHARMACY",
            "DISCHARGED"
    );
    private static final Set<String> ALLOWED_VISIT_TYPES = Set.of("NORMAL", "EMERGENCY");

    private final PatientVisitRepository patientVisitRepository;
    private final PatientRepository patientRepository;
    private final TenantAccessService tenantAccessService;

    public PatientVisitService(PatientVisitRepository patientVisitRepository,
                               PatientRepository patientRepository,
                               TenantAccessService tenantAccessService) {
        this.patientVisitRepository = patientVisitRepository;
        this.patientRepository = patientRepository;
        this.tenantAccessService = tenantAccessService;
    }

    public PatientVisitResponseDto startVisit(PatientVisitStartRequestDto dto, String hospitalId) {
        String effectiveHospitalId = tenantAccessService.normalizeHospitalScopeId(hospitalId);
        Patient patient = resolveScopedPatient(dto.patientId());

        PatientVisit visit = new PatientVisit();
        visit.setPatientId(String.valueOf(patient.getId()));
        visit.setReasonForVisit(trimToNull(dto.reasonForVisit()));
        visit.setStatus(normalizeStatus(dto.status(), "WAITING_TRIAGE"));
        visit.setVisitType(normalizeVisitType(dto.visitType(), "NORMAL"));
        visit.setAssignedDepartmentId(trimToNull(dto.assignedDepartmentId()));
        visit.setAssignedDepartmentName(trimToNull(dto.assignedDepartmentName()));
        visit.setAssignedDoctorId(firstNonBlank(dto.assignedDoctorId(), dto.doctorId()));
        visit.setAssignedDoctorName(firstNonBlank(dto.assignedDoctorName(), dto.doctorName()));
        visit.setAssignedNurseId(firstNonBlank(dto.assignedNurseId(), dto.nurseId()));
        visit.setAssignedNurseName(firstNonBlank(dto.assignedNurseName(), dto.nurseName()));
        visit.setDoctorId(firstNonBlank(dto.doctorId(), dto.assignedDoctorId()));
        visit.setDoctorName(firstNonBlank(dto.doctorName(), dto.assignedDoctorName()));
        visit.setNurseId(firstNonBlank(dto.nurseId(), dto.assignedNurseId()));
        visit.setNurseName(firstNonBlank(dto.nurseName(), dto.assignedNurseName()));
        visit.setDepartmentId(firstNonBlank(dto.departmentId(), dto.assignedDepartmentId()));
        visit.setDepartmentName(firstNonBlank(dto.departmentName(), dto.assignedDepartmentName()));
        visit.setAssignedTo(trimToNull(dto.assignedTo()));
        visit.setAssignedType(trimToNull(dto.assignedType()));
        visit.setEmergencyPriority(trimToNull(dto.emergencyPriority()));
        visit.setEmergencyStage(trimToNull(dto.emergencyStage()));
        visit.setHospitalId(effectiveHospitalId);

        if ("EMERGENCY".equals(visit.getVisitType())) {
            if (!StringUtils.hasText(visit.getAssignedDepartmentName())) {
                visit.setAssignedDepartmentName("Emergency Room");
            }
            if (!StringUtils.hasText(visit.getDepartmentName())) {
                visit.setDepartmentName("Emergency Room");
            }
            if (!StringUtils.hasText(visit.getAssignedTo())) {
                visit.setAssignedTo("Emergency Room");
            }
            if (!StringUtils.hasText(visit.getAssignedType())) {
                visit.setAssignedType("emergency");
            }
            if (!StringUtils.hasText(visit.getEmergencyStage())) {
                visit.setEmergencyStage("WAITING_ASSESSMENT");
            }
        }

        String nowEpoch = nowEpochSeconds();
        visit.setCreatedAt(nowEpoch);
        visit.setUpdatedAt(nowEpoch);

        return PatientVisitResponseDto.from(patientVisitRepository.save(visit), patient);
    }

    @Transactional(readOnly = true)
    public List<PatientVisitResponseDto> listVisits(String status, String visitType) {
        String normalizedStatus = StringUtils.hasText(status) ? normalizeStatus(status, null) : null;
        String normalizedVisitType = StringUtils.hasText(visitType) ? normalizeVisitType(visitType, null) : null;
        List<PatientVisit> visits = patientVisitRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));

        List<PatientVisit> visibleVisits = visits.stream()
                .filter(this::isVisibleToCurrentTenant)
                .filter(visit -> normalizedStatus == null || normalizedStatus.equalsIgnoreCase(visit.getStatus()))
                .filter(visit -> normalizedVisitType == null || normalizedVisitType.equalsIgnoreCase(defaultVisitType(visit.getVisitType())))
                .toList();

        return mapVisits(visibleVisits);
    }

    @Transactional(readOnly = true)
    public PatientVisitResponseDto getVisit(Long id) {
        PatientVisit visit = findScopedVisit(id);
        return PatientVisitResponseDto.from(visit, resolvePatientOrNull(visit.getPatientId()));
    }

    public PatientVisitResponseDto updateVisitStatus(Long visitId,
                                                     PatientVisitStatusUpdateRequestDto dto,
                                                     String hospitalId) {
        PatientVisit visit = findScopedVisit(visitId);

        if (StringUtils.hasText(dto.patientId()) && !dto.patientId().trim().equals(visit.getPatientId())) {
            throw new BadRequestException("The supplied patientId does not match this visit.");
        }

        visit.setStatus(normalizeStatus(dto.newStatus(), visit.getStatus()));
        applyStatusFields(visit, dto);
        visit.setVisitType(normalizeVisitType(dto.visitType(), defaultVisitType(visit.getVisitType())));
        if ("EMERGENCY".equals(defaultVisitType(visit.getVisitType())) && "DISCHARGED".equals(visit.getStatus())) {
            visit.setEmergencyCompletedAt(nowEpochSeconds());
            if (!StringUtils.hasText(visit.getEmergencyStage())) {
                visit.setEmergencyStage("DISCHARGED");
            }
        }
        visit.setUpdatedAt(nowEpochSeconds());

        Patient patient = resolvePatientOrNull(visit.getPatientId());
        return PatientVisitResponseDto.from(patientVisitRepository.save(visit), patient);
    }

    public PatientVisitResponseDto updateVisitStatus(PatientVisitStatusUpdateRequestDto dto, String hospitalId) {
        Long visitId;
        try {
            visitId = Long.parseLong(String.valueOf(dto.visitId()).trim());
        } catch (Exception ex) {
            throw new BadRequestException("A valid visitId is required.");
        }

        return updateVisitStatus(visitId, dto, hospitalId);
    }

    private List<PatientVisitResponseDto> mapVisits(List<PatientVisit> visits) {
        Map<Integer, Patient> patientMap = new HashMap<>();
        List<Integer> patientIds = visits.stream()
                .map(PatientVisit::getPatientId)
                .map(this::parseInteger)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (!patientIds.isEmpty()) {
            patientRepository.findAllById(patientIds).forEach(patient -> patientMap.put(patient.getId(), patient));
        }

        return visits.stream()
                .map(visit -> PatientVisitResponseDto.from(visit, patientMap.get(parseInteger(visit.getPatientId()))))
                .toList();
    }

    private void applyStatusFields(PatientVisit visit, PatientVisitStatusUpdateRequestDto dto) {
        if (StringUtils.hasText(dto.reasonForVisit())) visit.setReasonForVisit(dto.reasonForVisit().trim());
        if (StringUtils.hasText(dto.assignedDepartmentId())) visit.setAssignedDepartmentId(dto.assignedDepartmentId().trim());
        if (StringUtils.hasText(dto.assignedDepartmentName())) visit.setAssignedDepartmentName(dto.assignedDepartmentName().trim());
        if (StringUtils.hasText(dto.assignedDoctorId())) visit.setAssignedDoctorId(dto.assignedDoctorId().trim());
        if (StringUtils.hasText(dto.assignedDoctorName())) visit.setAssignedDoctorName(dto.assignedDoctorName().trim());
        if (StringUtils.hasText(dto.assignedNurseId())) visit.setAssignedNurseId(dto.assignedNurseId().trim());
        if (StringUtils.hasText(dto.assignedNurseName())) visit.setAssignedNurseName(dto.assignedNurseName().trim());
        if (StringUtils.hasText(dto.doctorId())) visit.setDoctorId(dto.doctorId().trim());
        if (StringUtils.hasText(dto.doctorName())) visit.setDoctorName(dto.doctorName().trim());
        if (StringUtils.hasText(dto.nurseId())) visit.setNurseId(dto.nurseId().trim());
        if (StringUtils.hasText(dto.nurseName())) visit.setNurseName(dto.nurseName().trim());
        if (StringUtils.hasText(dto.departmentId())) visit.setDepartmentId(dto.departmentId().trim());
        if (StringUtils.hasText(dto.departmentName())) visit.setDepartmentName(dto.departmentName().trim());
        if (StringUtils.hasText(dto.prescriptionId())) visit.setPrescriptionId(dto.prescriptionId().trim());
        if (StringUtils.hasText(dto.labOrderId())) visit.setLabOrderId(dto.labOrderId().trim());
        if (dto.emergencyPriority() != null) visit.setEmergencyPriority(trimToNull(dto.emergencyPriority()));
        if (dto.emergencyAssessment() != null) visit.setEmergencyAssessment(trimToNull(dto.emergencyAssessment()));
        if (dto.emergencyTreatment() != null) visit.setEmergencyTreatment(trimToNull(dto.emergencyTreatment()));
        if (dto.emergencyDisposition() != null) visit.setEmergencyDisposition(trimToNull(dto.emergencyDisposition()));
        if (dto.emergencyDispositionNote() != null) visit.setEmergencyDispositionNote(trimToNull(dto.emergencyDispositionNote()));
        if (dto.emergencyStage() != null) visit.setEmergencyStage(trimToNull(dto.emergencyStage()));
        if (dto.emergencyStageNote() != null) visit.setEmergencyStageNote(trimToNull(dto.emergencyStageNote()));
        if (dto.emergencyMedicationSummary() != null) visit.setEmergencyMedicationSummary(trimToNull(dto.emergencyMedicationSummary()));
        if (dto.emergencyBillingAmount() != null) visit.setEmergencyBillingAmount(trimToNull(dto.emergencyBillingAmount()));
        if (dto.emergencyBillingNote() != null) visit.setEmergencyBillingNote(trimToNull(dto.emergencyBillingNote()));
        if (dto.emergencyReferralDestination() != null) visit.setEmergencyReferralDestination(trimToNull(dto.emergencyReferralDestination()));
        if (dto.emergencyFollowUpInstruction() != null) visit.setEmergencyFollowUpInstruction(trimToNull(dto.emergencyFollowUpInstruction()));
    }

    private Patient resolveScopedPatient(String patientId) {
        Integer parsedPatientId = parseInteger(patientId);
        if (parsedPatientId == null) {
            throw new BadRequestException("A valid patientId is required.");
        }

        Patient patient = patientRepository.findById(parsedPatientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", patientId));

        if (!tenantAccessService.isSuperAdmin() && !tenantAccessService.belongsToCurrentScope(patient.getHospitalId())) {
            throw new ResourceNotFoundException("Patient", "id", patientId);
        }

        return patient;
    }

    private Patient resolvePatientOrNull(String patientId) {
        Integer parsedPatientId = parseInteger(patientId);
        if (parsedPatientId == null) return null;
        return patientRepository.findById(parsedPatientId).orElse(null);
    }

    private PatientVisit findScopedVisit(Long id) {
        PatientVisit visit = patientVisitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Visit", "id", id));

        if (!isVisibleToCurrentTenant(visit)) {
            throw new ResourceNotFoundException("Visit", "id", id);
        }

        return visit;
    }

    private boolean isVisibleToCurrentTenant(PatientVisit visit) {
        return tenantAccessService.isSuperAdmin()
                || tenantAccessService.belongsToCurrentScope(visit.getHospitalId())
                || (tenantAccessService.includeBlankHospitalRowsForCurrentUser()
                && !StringUtils.hasText(visit.getHospitalId()));
    }

    private String normalizeStatus(String status, String fallback) {
        String normalized = StringUtils.hasText(status) ? status.trim().toUpperCase(Locale.ROOT) : fallback;
        if (!StringUtils.hasText(normalized)) {
            throw new BadRequestException("Visit status is required.");
        }
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new BadRequestException("Unsupported visit status: " + normalized);
        }
        return normalized;
    }

    private String normalizeVisitType(String visitType, String fallback) {
        String normalized = StringUtils.hasText(visitType) ? visitType.trim().toUpperCase(Locale.ROOT) : fallback;
        if (!StringUtils.hasText(normalized)) {
            throw new BadRequestException("Visit type is required.");
        }
        if (!ALLOWED_VISIT_TYPES.contains(normalized)) {
            throw new BadRequestException("Unsupported visit type: " + normalized);
        }
        return normalized;
    }

    private String defaultVisitType(String visitType) {
        return StringUtils.hasText(visitType) ? visitType.trim().toUpperCase(Locale.ROOT) : "NORMAL";
    }

    private String nowEpochSeconds() {
        return String.valueOf(System.currentTimeMillis() / 1000L);
    }

    private Integer parseInteger(String value) {
        try {
            return StringUtils.hasText(value) ? Integer.parseInt(value.trim()) : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String firstNonBlank(String primary, String fallback) {
        return StringUtils.hasText(primary)
                ? primary.trim()
                : trimToNull(fallback);
    }
}
