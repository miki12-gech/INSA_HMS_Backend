package com.insa.hospital.service;

import com.insa.hospital.dto.PrescriptionRequestDto;
import com.insa.hospital.dto.PrescriptionResponseDto;
import com.insa.hospital.entity.Prescription;
import com.insa.hospital.exception.ResourceNotFoundException;
import com.insa.hospital.repository.PrescriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Prescription Service — Business Logic Layer
 *
 * KEY RESPONSIBILITY: Serialize/deserialize the legacy medicine
 * delimited string format (medicineId***dosage***freq***dur***notes###...)
 *
 * The @Transactional annotation on createPrescription ensures that if
 * serialization or save fails at any point, the entire operation rolls back.
 */
@Service
@Transactional
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final TenantAccessService tenantAccessService;

    @Autowired
    public PrescriptionService(PrescriptionRepository prescriptionRepository,
                               TenantAccessService tenantAccessService) {
        this.prescriptionRepository = prescriptionRepository;
        this.tenantAccessService = tenantAccessService;
    }

    // ─── Create ───────────────────────────────────────────────────────────────

    /**
     * Creates a prescription.
     * @Transactional: if the medicine serialization or DB save fails,
     * the entire operation is rolled back atomically.
     */
    @Transactional
    public PrescriptionResponseDto createPrescription(PrescriptionRequestDto dto,
                                                       String hospitalId,
                                                       String doctorId) {
        String effectiveHospitalId = tenantAccessService.normalizeHospitalScopeId(hospitalId);
        Prescription p = new Prescription();
        p.setPatient(dto.patient());
        p.setDoctor(StringUtils.hasText(doctorId) ? doctorId : "0");
        p.setHospitalId(effectiveHospitalId);
        p.setSymptom(dto.symptom());
        p.setNote(dto.note());
        p.setAdvice(dto.advice());
        p.setValidity(dto.validity());

        // Auto-set date if not provided
        String date = StringUtils.hasText(dto.date())
                ? dto.date()
                : String.valueOf(System.currentTimeMillis() / 1000L);
        p.setDate(date);
        // ── Serialize medicine list → legacy delimited string ─────────────────
        // Format: medicineId***dosage***frequency***duration***instructions###...
        String medicineStr = serializeMedicines(dto);
        p.setMedicine(medicineStr);
        p.setState(medicineStr.isEmpty() ? "COMPLETED" : "PENDING");

        return PrescriptionResponseDto.from(prescriptionRepository.save(p));
    }

    // ─── List / Get ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<PrescriptionResponseDto> listAll(String hospitalId, Pageable pageable) {
        if (tenantAccessService.isSuperAdmin()) {
            return prescriptionRepository.findAll(pageable).map(PrescriptionResponseDto::from);
        }

        return prescriptionRepository.findByHospitalId(tenantAccessService.getCurrentHospitalId(), pageable)
                .map(PrescriptionResponseDto::from);
    }

    @Transactional(readOnly = true)
    public List<PrescriptionResponseDto> listByPatient(String patientId, String hospitalId) {
        return listByPatientVisibleEntities(patientId)
                .stream()
                .map(PrescriptionResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<PrescriptionResponseDto> listByDoctor(String doctorId, String hospitalId, Pageable pageable) {
        if (tenantAccessService.isSuperAdmin()) {
            return prescriptionRepository.findAll(pageable)
                    .map(PrescriptionResponseDto::from);
        }

        return prescriptionRepository.findVisibleByDoctor(
                        doctorId,
                        tenantAccessService.getCurrentHospitalScopeIds(),
                        tenantAccessService.includeBlankHospitalRowsForCurrentUser(),
                        pageable)
                .map(PrescriptionResponseDto::from);
    }

    @Transactional(readOnly = true)
    public List<Prescription> listByDoctorVisibleEntities(String doctorId) {
        if (tenantAccessService.isSuperAdmin()) {
            return prescriptionRepository.findAll().stream()
                    .filter(p -> doctorId.equals(p.getDoctor()))
                    .sorted(java.util.Comparator.comparing(Prescription::getId).reversed())
                    .toList();
        }

        return prescriptionRepository.findVisibleByDoctor(
                        doctorId,
                        tenantAccessService.getCurrentHospitalScopeIds(),
                        tenantAccessService.includeBlankHospitalRowsForCurrentUser(),
                        Pageable.unpaged())
                .getContent();
    }

    @Transactional(readOnly = true)
    public PrescriptionResponseDto getById(Integer id, String hospitalId) {
        Prescription p = prescriptionRepository.findById(id)
                .filter(pr -> tenantAccessService.isSuperAdmin()
                        || tenantAccessService.belongsToCurrentScope(pr.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Prescription", "id", id));
        return PrescriptionResponseDto.from(p);
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    @Transactional
    public PrescriptionResponseDto updatePrescription(Integer id,
                                                       PrescriptionRequestDto dto,
                                                       String hospitalId,
                                                       String doctorId) {
        Prescription p = prescriptionRepository.findById(id)
                .filter(pr -> tenantAccessService.isSuperAdmin()
                        || tenantAccessService.belongsToCurrentScope(pr.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Prescription", "id", id));

        p.setSymptom(dto.symptom());
        p.setNote(dto.note());
        p.setAdvice(dto.advice());
        p.setValidity(dto.validity());
        p.setMedicine(serializeMedicines(dto));

        return PrescriptionResponseDto.from(prescriptionRepository.save(p));
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    @Transactional
    public void deletePrescription(Integer id, String hospitalId) {
        Prescription p = prescriptionRepository.findById(id)
                .filter(pr -> tenantAccessService.isSuperAdmin()
                        || tenantAccessService.belongsToCurrentScope(pr.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Prescription", "id", id));
        prescriptionRepository.delete(p);
    }

    @Transactional(readOnly = true)
    public List<Prescription> listAllVisibleEntities() {
        if (tenantAccessService.isSuperAdmin()) {
            return prescriptionRepository.findAll().stream()
                    .sorted(java.util.Comparator.comparing(Prescription::getId).reversed())
                    .toList();
        }

        return prescriptionRepository.findVisibleByHospitalIdsOrderByIdDesc(
                tenantAccessService.getCurrentHospitalScopeIds(),
                tenantAccessService.includeBlankHospitalRowsForCurrentUser()
        );
    }

    @Transactional(readOnly = true)
    public List<Prescription> listPendingVisibleEntities(List<String> excludedStates) {
        if (tenantAccessService.isSuperAdmin()) {
            return prescriptionRepository.findAll().stream()
                    .filter(p -> p.getState() == null || !excludedStates.contains(p.getState()))
                    .sorted(java.util.Comparator.comparing(Prescription::getId).reversed())
                    .toList();
        }

        return prescriptionRepository.findVisiblePendingByHospitalIds(
                tenantAccessService.getCurrentHospitalScopeIds(),
                tenantAccessService.includeBlankHospitalRowsForCurrentUser(),
                excludedStates
        );
    }

    @Transactional(readOnly = true)
    public List<Prescription> listByPatientVisibleEntities(String patientId) {
        if (tenantAccessService.isSuperAdmin()) {
            return prescriptionRepository.findAll().stream()
                    .filter(p -> patientId.equals(p.getPatient()))
                    .sorted(java.util.Comparator.comparing(Prescription::getId).reversed())
                    .toList();
        }

        return prescriptionRepository.findVisibleByPatient(
                patientId,
                tenantAccessService.getCurrentHospitalScopeIds(),
                tenantAccessService.includeBlankHospitalRowsForCurrentUser()
        );
    }

    // ─── Private: Medicine String Serialization ───────────────────────────────

    /**
     * Converts the structured medicine list from the DTO into the legacy
     * delimited string stored in prescription.medicine:
     *   "medicineId***dosage***frequency***duration***instructions###medicineId2***..."
     */
    private String serializeMedicines(PrescriptionRequestDto dto) {
        if (dto.medicines() == null || dto.medicines().isEmpty()) {
            return "";
        }
        return dto.medicines().stream()
            .map(item -> String.join(
                    Prescription.FIELD_SEPARATOR,
                    String.valueOf(item.medicineId()),
                    nvl(item.dosage()),
                    nvl(item.frequency()),
                    nvl(item.duration()),
                    nvl(item.instructions())
            ))
            .collect(Collectors.joining(Prescription.MEDICINE_SEPARATOR));
    }

    private String nvl(String s) {
        return s != null ? s : "";
    }
}
