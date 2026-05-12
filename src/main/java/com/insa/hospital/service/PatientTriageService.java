package com.insa.hospital.service;

import com.insa.hospital.dto.PatientTriageRequestDto;
import com.insa.hospital.dto.PatientTriageResponseDto;
import com.insa.hospital.entity.Patient;
import com.insa.hospital.entity.PatientTriage;
import com.insa.hospital.exception.ResourceNotFoundException;
import com.insa.hospital.repository.PatientRepository;
import com.insa.hospital.repository.PatientTriageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * PatientTriage (Vitals) Service — Business Logic Layer
 *
 * Key responsibilities:
 *  1. Record new triage/vitals for a patient.
 *  2. Auto-set date (Unix epoch) and date_string (legacy human-readable format)
 *     at save time from the current system clock.
 *  3. Auto-resolve and denormalize patient_name from the patient table.
 *  4. Enforce multi-tenant scoping on every query.
 */
@Service
@Transactional
public class PatientTriageService {

    /** Legacy date_string format from actual data: "10 January 2024 - 08:50 PM" */
    private static final DateTimeFormatter DATE_STRING_FMT =
            DateTimeFormatter.ofPattern("dd MMMM yyyy - hh:mm a");

    private final PatientTriageRepository triageRepository;
    private final PatientRepository patientRepository;
    private final TenantAccessService tenantAccessService;

    @Autowired
    public PatientTriageService(PatientTriageRepository triageRepository,
                                PatientRepository patientRepository,
                                TenantAccessService tenantAccessService) {
        this.triageRepository = triageRepository;
        this.patientRepository = patientRepository;
        this.tenantAccessService = tenantAccessService;
    }

    // ─── Record Triage ────────────────────────────────────────────────────────

    /**
     * Records a new triage/vitals entry for a patient.
     * Auto-fills date (Unix epoch), date_string (human-readable), patient_name.
     */
    public PatientTriageResponseDto recordTriage(PatientTriageRequestDto dto, String hospitalId) {
        String effectiveHospitalId = tenantAccessService.normalizeHospitalScopeId(hospitalId);
        PatientTriage triage = new PatientTriage();

        triage.setPatient(dto.patient());
        triage.setTitle(dto.title());
        triage.setBloodPressure(dto.blood_pressure());
        triage.setHeatBeat(dto.heatBeat());
        triage.setOxygenSaturation(dto.oxygenSaturation());
        triage.setSugerlevel(dto.sugerlevel());
        triage.setHeight(dto.height());
        triage.setWeight(dto.weight());
        triage.setTemperature(dto.temperature());
        triage.setResparatoryRate(dto.resparatoryRate());
        triage.setUrl(dto.url() != null ? dto.url() : "");
        triage.setHospitalId(effectiveHospitalId);

        // ── Auto-set timestamps ───────────────────────────────────────────────
        LocalDateTime now = LocalDateTime.now();
        triage.setDate(String.valueOf(System.currentTimeMillis() / 1000L));   // Unix epoch
        triage.setDateString(now.format(DATE_STRING_FMT));                    // "10 January 2024 - 08:50 PM"

        // ── Denormalize patient name (legacy pattern: store name at triage time) ─
        String patientName = resolvePatientName(dto.patient());
        triage.setPatientName(patientName);

        return PatientTriageResponseDto.from(triageRepository.save(triage));
    }

    // ─── List ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<PatientTriageResponseDto> listTriage(String name, String startDate, String endDate, String hospitalId, Pageable pageable) {
        String startEpoch = null;
        String endEpoch = null;

        try {
            if (StringUtils.hasText(startDate)) {
                startEpoch = String.valueOf(java.time.LocalDate.parse(startDate).atStartOfDay(java.time.ZoneId.systemDefault()).toEpochSecond());
            }
            if (StringUtils.hasText(endDate)) {
                endEpoch = String.valueOf(java.time.LocalDate.parse(endDate).plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toEpochSecond() - 1);
            }
        } catch (Exception e) {
            // Leave as null if parsing fails
        }

        if (tenantAccessService.isSuperAdmin()) {
            if (!StringUtils.hasText(name) && startEpoch == null && endEpoch == null) {
                return triageRepository.findAll(pageable).map(PatientTriageResponseDto::from);
            }

            return triageRepository.findVisibleFilteredTriage(
                    tenantAccessService.getAllHospitalScopeIds(),
                    true,
                    name,
                    startEpoch,
                    endEpoch,
                    pageable
            ).map(PatientTriageResponseDto::from);
        }

        if (!StringUtils.hasText(name) && startEpoch == null && endEpoch == null) {
            return triageRepository.findVisibleByHospitalIds(
                    tenantAccessService.getCurrentHospitalScopeIds(),
                    tenantAccessService.includeBlankHospitalRowsForCurrentUser(),
                    pageable
            ).map(PatientTriageResponseDto::from);
        }

        return triageRepository.findVisibleFilteredTriage(
                        tenantAccessService.getCurrentHospitalScopeIds(),
                        tenantAccessService.includeBlankHospitalRowsForCurrentUser(),
                        name,
                        startEpoch,
                        endEpoch,
                        pageable)
                .map(PatientTriageResponseDto::from);
    }

    @Transactional(readOnly = true)
    public List<PatientTriageResponseDto> listByPatient(String patientId, String hospitalId) {
        if (tenantAccessService.isSuperAdmin()) {
            return triageRepository.findAll().stream()
                    .filter(t -> patientId.equals(t.getPatient()))
                    .map(PatientTriageResponseDto::from)
                    .toList();
        }

        return triageRepository.findVisibleByPatient(
                        patientId,
                        tenantAccessService.getCurrentHospitalScopeIds(),
                        tenantAccessService.includeBlankHospitalRowsForCurrentUser())
                .stream()
                .map(PatientTriageResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PatientTriageResponseDto getLatestByPatient(String patientId, String hospitalId) {
        Pageable top1 = PageRequest.of(0, 1);
        List<PatientTriage> rows = tenantAccessService.isSuperAdmin()
                ? triageRepository.findAll().stream()
                    .filter(t -> patientId.equals(t.getPatient()))
                    .sorted(java.util.Comparator.comparing(PatientTriage::getId).reversed())
                    .limit(1)
                    .toList()
                : triageRepository.findVisibleLatestByPatient(
                        patientId,
                        tenantAccessService.getCurrentHospitalScopeIds(),
                        tenantAccessService.includeBlankHospitalRowsForCurrentUser(),
                        top1);
        return rows
                .stream()
                .map(PatientTriageResponseDto::from)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Triage", "patientId", patientId));
    }

    // ─── Get / Update / Delete ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PatientTriageResponseDto getTriageById(Integer id, String hospitalId) {
        PatientTriage triage = triageRepository.findById(id)
                .filter(t -> tenantAccessService.isSuperAdmin()
                        || tenantAccessService.belongsToCurrentScope(t.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("PatientTriage", "id", id));
        return PatientTriageResponseDto.from(triage);
    }

    public PatientTriageResponseDto updateTriage(Integer id,
                                                  PatientTriageRequestDto dto,
                                                  String hospitalId) {
        PatientTriage triage = triageRepository.findById(id)
                .filter(t -> tenantAccessService.isSuperAdmin()
                        || tenantAccessService.belongsToCurrentScope(t.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("PatientTriage", "id", id));

        triage.setTitle(dto.title());
        triage.setBloodPressure(dto.blood_pressure());
        triage.setHeatBeat(dto.heatBeat());
        triage.setOxygenSaturation(dto.oxygenSaturation());
        triage.setSugerlevel(dto.sugerlevel());
        triage.setHeight(dto.height());
        triage.setWeight(dto.weight());
        triage.setTemperature(dto.temperature());
        triage.setResparatoryRate(dto.resparatoryRate());
        if (StringUtils.hasText(dto.url())) {
            triage.setUrl(dto.url());
        }

        return PatientTriageResponseDto.from(triageRepository.save(triage));
    }

    public void deleteTriage(Integer id, String hospitalId) {
        PatientTriage triage = triageRepository.findById(id)
                .filter(t -> tenantAccessService.isSuperAdmin()
                        || tenantAccessService.belongsToCurrentScope(t.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("PatientTriage", "id", id));
        triageRepository.delete(triage);
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────

    private String resolvePatientName(String patientId) {
        if (!StringUtils.hasText(patientId)) return "";
        try {
            return patientRepository.findById(Integer.parseInt(patientId))
                    .map(Patient::getName)
                    .orElse("");
        } catch (NumberFormatException e) {
            return "";
        }
    }
}
