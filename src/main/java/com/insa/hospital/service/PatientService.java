package com.insa.hospital.service;

import com.insa.hospital.dto.PatientRequestDto;
import com.insa.hospital.dto.PatientResponseDto;
import com.insa.hospital.entity.Hospital;
import com.insa.hospital.entity.Patient;
import com.insa.hospital.exception.LimitExceededException;
import com.insa.hospital.exception.ResourceNotFoundException;
import com.insa.hospital.repository.HospitalRepository;
import com.insa.hospital.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * Patient Service — Business Logic Layer
 *
 * Key responsibilities:
 *  1. Map DTO → Entity for registration.
 *  2. AGE → BIRTHDATE calculation:
 *     If 'age' is provided but 'birthdate' is absent, compute the birthdate
 *     as January 1st of (currentYear - age), formatted as "DD-MM-YYYY".
 *     This mirrors the receptionist's requested workflow.
 *  3. Generate a random 6-digit patientId (like the legacy PHP system).
 *  4. Set add_date (MM/DD/YY) and registration_time (Unix epoch) at save time.
 *  5. Enforce multi-tenancy: every query is scoped to the caller's hospitalId.
 */
@Service
@Transactional
public class PatientService {

    // Legacy date formatters — must match the formats in the SQL dump exactly
    private static final DateTimeFormatter BIRTHDATE_FMT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");   // "07-07-2019"
    private static final DateTimeFormatter ADD_DATE_FMT =
            DateTimeFormatter.ofPattern("MM/dd/yy");     // "07/07/19"

    private final PatientRepository patientRepository;
    private final HospitalRepository hospitalRepository;
    private final TenantAccessService tenantAccessService;
    private final Random random = new Random();

    @Autowired
    public PatientService(PatientRepository patientRepository,
                          HospitalRepository hospitalRepository,
                          TenantAccessService tenantAccessService) {
        this.patientRepository = patientRepository;
        this.hospitalRepository = hospitalRepository;
        this.tenantAccessService = tenantAccessService;
    }

    // ─── Register Patient ─────────────────────────────────────────────────────

    /**
     * Registers a new patient.
     *
     * @param dto        Validated request body from the controller.
     * @param hospitalId Resolved from the JWT token (multi-tenant).
     * @param ionUserId  Resolved from the JWT token (the logged-in user's legacy id).
     */
    public PatientResponseDto registerPatient(PatientRequestDto dto,
                                               String hospitalId,
                                               String ionUserId) {
        String effectiveHospitalId = tenantAccessService.normalizeHospitalScopeId(hospitalId);

        // ══════════════════════════════════════════════════════════════
        //  AUDIT FIX — Alert 6: SaaS Patient Roster Limit Enforcement
        // ══════════════════════════════════════════════════════════════
        //  Legacy source: doctor/controllers/doctor.php line 44
        //    $limit = $this->doctor_model->getLimit();
        //    if ($limit <= 0) { redirect with error; }
        //
        //  hospital.p_limit stores the max allowed patients as varchar.
        //  If p_limit is null or 0, the limit is treated as unlimited.
        // ══════════════════════════════════════════════════════════════
        Hospital hospital = hospitalRepository.findByIonUserId(effectiveHospitalId)
                .orElseGet(() -> {
                    try {
                        return hospitalRepository.findById(Integer.parseInt(effectiveHospitalId)).orElse(null);
                    } catch (NumberFormatException ignored) {
                        return null;
                    }
                });
        if (hospital != null && StringUtils.hasText(hospital.getPLimit())) {
            try {
                long limit = Long.parseLong(hospital.getPLimit().trim());
                if (limit > 0) {
                    long current = patientRepository.countByHospitalId(effectiveHospitalId);
                    if (current >= limit) {
                        throw new LimitExceededException("Patient", current, limit);
                    }
                }
            } catch (NumberFormatException ignored) { /* non-numeric limit = unlimited */ }
        }

        Patient patient = new Patient();

        // ── Basic fields ──────────────────────────────────────────────────────
        patient.setName(dto.name());
        patient.setEmail(dto.email());
        patient.setPhone(dto.phone());
        patient.setAddress(dto.address());
        patient.setSex(dto.sex());
        patient.setBloodgroup(dto.bloodgroup());
        patient.setMembershiptype(dto.membershiptype());
        patient.setDepId(dto.depId());
        patient.setAllergynote(dto.allergynote());
        patient.setDoctor(dto.doctor());
        patient.setImgUrl(dto.imgUrl());
        patient.setHowAdded("from_pos"); // default — receptionist registration

        // ── Multi-tenant fields ───────────────────────────────────────────────
        patient.setHospitalId(effectiveHospitalId);
        patient.setIonUserId(ionUserId);

        // ── Age / Birthdate Logic (Receptionist Requested Feature) ─────────────
        // Rule: If 'birthdate' is given, store it and calculate age from it.
        //       If only 'age' is given, calculate and store the birthdate.
        //       If both are given, birthdate takes precedence.
        resolveBirthdateAndAge(patient, dto.birthdate(), dto.age());

        // ── Date / Time fields ────────────────────────────────────────────────
        LocalDate today = LocalDate.now();
        patient.setAddDate(today.format(ADD_DATE_FMT));               // "03/18/26"
        patient.setRegistrationTime(                                   // Unix epoch (seconds)
                String.valueOf(System.currentTimeMillis() / 1000L));

        // ── Generate unique 6-digit patient_id (legacy behaviour) ─────────────
        patient.setPatientId(generateUniquePatientId());

        Patient saved = patientRepository.save(patient);
        return PatientResponseDto.from(saved);
    }

    // ─── List / Search ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<PatientResponseDto> listPatients(String hospitalId, Pageable pageable) {
        if (tenantAccessService.isSuperAdmin()) {
            return patientRepository.findAll(pageable).map(PatientResponseDto::from);
        }

        return patientRepository.findVisibleByHospitalIds(
                        tenantAccessService.getCurrentHospitalScopeIds(),
                        tenantAccessService.includeBlankHospitalRowsForCurrentUser(),
                        pageable)
                .map(PatientResponseDto::from);
    }

    @Transactional(readOnly = true)
    public java.util.List<PatientResponseDto> getAllPatients(String hospitalId) {
        if (tenantAccessService.isSuperAdmin()) {
            return patientRepository.findAll()
                    .stream()
                    .map(PatientResponseDto::from)
                    .toList();
        }

        return patientRepository.findVisibleByHospitalIds(
                        tenantAccessService.getCurrentHospitalScopeIds(),
                        tenantAccessService.includeBlankHospitalRowsForCurrentUser())
                .stream()
                .map(PatientResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<PatientResponseDto> searchPatients(String hospitalId,
                                                    String query,
                                                    Pageable pageable) {
        if (tenantAccessService.isSuperAdmin()) {
            return patientRepository.searchVisibleByNameOrPhoneOrPatientId(
                    tenantAccessService.getAllHospitalScopeIds(),
                    true,
                    query,
                    pageable
            ).map(PatientResponseDto::from);
        }

        return patientRepository.searchVisibleByNameOrPhoneOrPatientId(
                        tenantAccessService.getCurrentHospitalScopeIds(),
                        tenantAccessService.includeBlankHospitalRowsForCurrentUser(),
                        query,
                        pageable)
                .map(PatientResponseDto::from);
    }

    @Transactional(readOnly = true)
    public PatientResponseDto getPatientById(Integer id, String hospitalId) {
        Patient patient = patientRepository.findById(id)
                .filter(p -> tenantAccessService.isSuperAdmin()
                        || tenantAccessService.belongsToCurrentScope(p.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", id));
        return PatientResponseDto.from(patient);
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    public PatientResponseDto updatePatient(Integer id,
                                             PatientRequestDto dto,
                                             String hospitalId) {
        Patient patient = patientRepository.findById(id)
                .filter(p -> tenantAccessService.isSuperAdmin()
                        || tenantAccessService.belongsToCurrentScope(p.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", id));

        patient.setName(dto.name());
        patient.setEmail(dto.email());
        patient.setPhone(dto.phone());
        patient.setAddress(dto.address());
        patient.setSex(dto.sex());
        patient.setBloodgroup(dto.bloodgroup());
        patient.setMembershiptype(dto.membershiptype());
        patient.setDepId(dto.depId());
        patient.setAllergynote(dto.allergynote());
        patient.setDoctor(dto.doctor());

        if (StringUtils.hasText(dto.imgUrl())) {
            patient.setImgUrl(dto.imgUrl());
        }

        resolveBirthdateAndAge(patient, dto.birthdate(), dto.age());

        return PatientResponseDto.from(patientRepository.save(patient));
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    public void deletePatient(Integer id, String hospitalId) {
        Patient patient = patientRepository.findById(id)
                .filter(p -> tenantAccessService.isSuperAdmin()
                        || tenantAccessService.belongsToCurrentScope(p.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", id));
        patientRepository.delete(patient);
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────

    /**
     * Resolves the birthdate and age fields on the entity.
     *
     * Priority logic:
     *  1. birthdate provided  → parse it, store it, calculate age from it.
     *  2. only age provided   → calculate birthdate as 01-01-(currentYear - age).
     *  3. neither provided    → leave both blank (null/empty, matching legacy).
     */
    private void resolveBirthdateAndAge(Patient patient,
                                         String birthdateInput,
                                         String ageInput) {
        LocalDate today = LocalDate.now();

        if (StringUtils.hasText(birthdateInput)) {
            // Birthdate given — store as-is, calculate age
            patient.setBirthdate(birthdateInput);
            try {
                LocalDate dob = LocalDate.parse(birthdateInput, BIRTHDATE_FMT);
                int calculatedAge = today.getYear() - dob.getYear();
                // Adjust if birthday hasn't occurred yet this year
                if (today.getMonthValue() < dob.getMonthValue() ||
                        (today.getMonthValue() == dob.getMonthValue()
                                && today.getDayOfMonth() < dob.getDayOfMonth())) {
                    calculatedAge--;
                }
                patient.setAge(String.valueOf(calculatedAge));
            } catch (Exception e) {
                // Unparseable date — store age as provided (legacy tolerance)
                patient.setAge(ageInput != null ? ageInput : "");
            }

        } else if (StringUtils.hasText(ageInput)) {
            // ── RECEPTIONIST FEATURE: Age provided → auto-calculate birthdate ──
            // Store the age string directly
            patient.setAge(ageInput);
            try {
                int age = Integer.parseInt(ageInput.trim());
                // Calculate birth year; default to January 1st (legacy convention)
                int birthYear = today.getYear() - age;
                LocalDate estimatedDob = LocalDate.of(birthYear, 1, 1);
                patient.setBirthdate(estimatedDob.format(BIRTHDATE_FMT)); // "01-01-YYYY"
            } catch (NumberFormatException e) {
                // Non-numeric age value — store as blank (legacy tolerance)
                patient.setBirthdate("");
            }

        } else {
            // Neither provided — match legacy behaviour (empty strings)
            patient.setBirthdate("");
            patient.setAge("");
        }
    }

    /**
     * Generates a unique 6-digit patient_id, matching the legacy PHP random number.
     * Retries up to 10 times if a collision is found (extremely rare).
     */
    private String generateUniquePatientId() {
        for (int attempt = 0; attempt < 10; attempt++) {
            // 100000 – 999999 inclusive (always 6 digits)
            int candidate = 100000 + random.nextInt(900000);
            String candidateStr = String.valueOf(candidate);
            if (!patientRepository.existsByPatientId(candidateStr)) {
                return candidateStr;
            }
        }
        // Extremely unlikely fallback — use timestamp suffix
        return String.valueOf(System.currentTimeMillis() % 1000000);
    }
}
