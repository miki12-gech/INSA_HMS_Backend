package com.insa.hospital.service;

import com.insa.hospital.dto.DoctorDhsReportResponseDto;
import com.insa.hospital.dto.DoctorRequestDto;
import com.insa.hospital.dto.DoctorResponseDto;
import com.insa.hospital.dto.MedicalHistoryDto;
import com.insa.hospital.dto.PatientMedicalHistoryResponseDto;
import com.insa.hospital.dto.PatientNoteDto;
import com.insa.hospital.dto.PatientTriageResponseDto;
import com.insa.hospital.entity.Doctor;
import com.insa.hospital.entity.Hospital;
import com.insa.hospital.entity.MedicalHistory;
import com.insa.hospital.entity.Patient;
import com.insa.hospital.entity.PatientNote;
import com.insa.hospital.entity.PatientVisit;
import com.insa.hospital.exception.LimitExceededException;
import com.insa.hospital.exception.ResourceNotFoundException;
import com.insa.hospital.repository.DoctorRepository;
import com.insa.hospital.repository.HospitalRepository;
import com.insa.hospital.repository.MedicalHistoryRepository;
import com.insa.hospital.repository.PatientRepository;
import com.insa.hospital.repository.PatientNoteRepository;
import com.insa.hospital.repository.PatientVisitRepository;
import com.insa.hospital.entity.User;
import com.insa.hospital.entity.UserGroup;
import com.insa.hospital.repository.PatientTriageRepository;
import com.insa.hospital.repository.UserRepository;
import com.insa.hospital.repository.UserGroupRepository;
import com.insa.hospital.util.StaffEmailPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Doctor Service — Business Logic Layer
 *
 * Handles CRUD for the legacy `doctor` table.
 * Every query is scoped by hospitalId (multi-tenancy from JWT).
 */
@Service
@Transactional
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final HospitalRepository hospitalRepository;
    private final MedicalHistoryRepository medicalHistoryRepository;
    private final PatientRepository patientRepository;
    private final PatientNoteRepository patientNoteRepository;
    private final PatientTriageRepository patientTriageRepository;
    private final PatientVisitRepository patientVisitRepository;
    private final PrescriptionService prescriptionService;
    private final UserRepository userRepository;
    private final UserGroupRepository userGroupRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DoctorService(DoctorRepository doctorRepository,
                         HospitalRepository hospitalRepository,
                         MedicalHistoryRepository medicalHistoryRepository,
                         PatientRepository patientRepository,
                         PatientNoteRepository patientNoteRepository,
                         PatientTriageRepository patientTriageRepository,
                         PatientVisitRepository patientVisitRepository,
                         PrescriptionService prescriptionService,
                         UserRepository userRepository,
                         UserGroupRepository userGroupRepository,
                         PasswordEncoder passwordEncoder) {
        this.doctorRepository = doctorRepository;
        this.hospitalRepository = hospitalRepository;
        this.medicalHistoryRepository = medicalHistoryRepository;
        this.patientRepository = patientRepository;
        this.patientNoteRepository = patientNoteRepository;
        this.patientTriageRepository = patientTriageRepository;
        this.patientVisitRepository = patientVisitRepository;
        this.prescriptionService = prescriptionService;
        this.userRepository = userRepository;
        this.userGroupRepository = userGroupRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ─── Create ───────────────────────────────────────────────────────────────

    public DoctorResponseDto createDoctor(DoctorRequestDto dto, String hospitalId) {
        String normalizedEmail = StaffEmailPolicy.normalizeStaffEmail(dto.email());

        // ══════════════════════════════════════════════════════════════
        //  AUDIT FIX — Alert 6: SaaS Doctor Roster Limit Enforcement
        // ══════════════════════════════════════════════════════════════
        //  Legacy source: doctor/controllers/doctor.php line 44
        //    $limit = $this->doctor_model->getLimit();
        //    if ($limit <= 0) { redirect with error; }
        //
        //  hospital.d_limit stores the max allowed doctors as varchar.
        //  If d_limit is null or 0, the limit is treated as unlimited.
        // ══════════════════════════════════════════════════════════════
        try {
            Hospital hospital = hospitalRepository.findById(Integer.parseInt(hospitalId))
                    .orElse(null);
            if (hospital != null && StringUtils.hasText(hospital.getDLimit())) {
                long limit = Long.parseLong(hospital.getDLimit().trim());
                if (limit > 0) {
                    long current = doctorRepository.countByHospitalId(hospitalId);
                    if (current >= limit) {
                        throw new LimitExceededException("Doctor", current, limit);
                    }
                }
            }
        } catch (LimitExceededException e) {
            throw e; // re-throw limit exceptions
        } catch (Exception ignored) { /* non-numeric hospitalId or limit = skip */ }

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new RuntimeException("This Email Address Is Already Registered");
        }

        // 1) Create User
        User user = new User();
        user.setUsername(dto.name());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setHospitalIonId(hospitalId);
        user.setActive(1); // From the legacy code typical ion_auth active=1
        user.setIpAddress("127.0.0.1"); // ip_address is NOT NULL in database
        user.setCreatedOn(System.currentTimeMillis() / 1000L); // Unix timestamp
        User savedUser = userRepository.save(user);

        // 2) Link to Group 4 (Doctor)
        UserGroup userGroup = new UserGroup();
        userGroup.setUserId(savedUser.getId());
        userGroup.setGroupId(4); // 4 = Doctor group in legacy
        userGroupRepository.save(userGroup);

        Doctor doctor = new Doctor();
        mapDtoToEntity(dto, doctor, normalizedEmail);
        doctor.setIonUserId(String.valueOf(savedUser.getId())); // Auto-filled from ion_auth
        doctor.setHospitalId(hospitalId);
        return DoctorResponseDto.from(doctorRepository.save(doctor));
    }

    // ─── List / Search ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<DoctorResponseDto> listDoctors(String hospitalId, Pageable pageable) {
        return doctorRepository.findByHospitalId(hospitalId, pageable)
                .map(DoctorResponseDto::from);
    }

    @Transactional(readOnly = true)
    public List<DoctorResponseDto> listAllDoctors(String hospitalId) {
        return doctorRepository.findByHospitalId(hospitalId)
                .stream()
                .map(DoctorResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<DoctorResponseDto> searchDoctors(String hospitalId, String query, Pageable pageable) {
        return doctorRepository.searchByNameOrDepartment(hospitalId, query, pageable)
                .map(DoctorResponseDto::from);
    }

    // ─── Get single ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public DoctorResponseDto getDoctorById(Integer id, String hospitalId) {
        Doctor doctor = doctorRepository.findById(id)
                .filter(d -> hospitalId.equals(d.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", id));
        return DoctorResponseDto.from(doctor);
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    public DoctorResponseDto updateDoctor(Integer id, DoctorRequestDto dto, String hospitalId) {
        String normalizedEmail = StaffEmailPolicy.normalizeStaffEmail(dto.email());
        Doctor doctor = doctorRepository.findById(id)
                .filter(d -> hospitalId.equals(d.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", id));

        // Sync underlying users table (email and optionally password)
        if (StringUtils.hasText(doctor.getIonUserId())) {
            userRepository.findById(Long.parseLong(doctor.getIonUserId())).ifPresent(user -> {
                user.setUsername(dto.name());
                user.setEmail(normalizedEmail);
                if (StringUtils.hasText(dto.password())) {
                    user.setPassword(passwordEncoder.encode(dto.password()));
                }
                userRepository.save(user);
            });
        }

        mapDtoToEntity(dto, doctor, normalizedEmail);
        return DoctorResponseDto.from(doctorRepository.save(doctor));
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    public void deleteDoctor(Integer id, String hospitalId) {
        Doctor doctor = doctorRepository.findById(id)
                .filter(d -> hospitalId.equals(d.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", id));
        
        if (StringUtils.hasText(doctor.getIonUserId())) {
            userRepository.deleteById(Long.parseLong(doctor.getIonUserId()));
        }
        
        doctorRepository.delete(doctor);
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────

    private void mapDtoToEntity(DoctorRequestDto dto, Doctor doctor, String normalizedEmail) {
        doctor.setName(dto.name());
        doctor.setEmail(normalizedEmail);
        doctor.setAddress(dto.address());
        doctor.setPhone(dto.phone());
        doctor.setDepartment(dto.department());
        doctor.setProfile(dto.profile());
        doctor.setX(dto.x());
        doctor.setY(dto.y());
        doctor.setIonUserId(dto.ionUserId());
        if (StringUtils.hasText(dto.imgUrl())) {
            doctor.setImgUrl(dto.imgUrl());
        }
    }

    // ─── Phase 6: Consultations / Medical History ─────────────────────────────

    public MedicalHistoryDto saveMedicalHistory(MedicalHistoryDto dto, String doctorId) {
        MedicalHistory history = new MedicalHistory();
        history.setPatientId(dto.getPatientId());
        history.setTitle(dto.getTitle());
        history.setDescription(dto.getDescription());
        history.setPatientName(dto.getPatientName());
        history.setPatientAddress(dto.getPatientAddress());
        history.setPatientPhone(dto.getPatientPhone());
        history.setImgUrl(dto.getImgUrl());
        history.setDate(StringUtils.hasText(dto.getDate()) ? dto.getDate() : String.valueOf(System.currentTimeMillis() / 1000L));
        history.setRegistrationTime(dto.getRegistrationTime());
        history.setHospitalId(dto.getHospitalId());
        history.setDiagnosisCategory(dto.getDiagnosisCategory());
        
        MedicalHistory saved = medicalHistoryRepository.save(history);
        dto.setId(saved.getId().toString()); // Assuming DTO should have it, otherwise ignore
        return dto; // Simplification matching the Dto
    }

    public PatientNoteDto savePatientNote(PatientNoteDto dto, String doctorId) {
        PatientNote note = new PatientNote();
        note.setPatientId(dto.getPatientId());
        note.setTitle(dto.getTitle());
        note.setDescription(dto.getDescription());
        note.setPatientName(dto.getPatientName());
        note.setImgUrl(dto.getImgUrl());
        note.setDate(StringUtils.hasText(dto.getDate()) ? dto.getDate() : String.valueOf(System.currentTimeMillis() / 1000L));
        note.setRegistrationTime(dto.getRegistrationTime());
        note.setStatus(dto.getStatus());
        note.setDateString(dto.getDateString());
        note.setDatetimeString(dto.getDatetimeString());
        note.setDoctorName(dto.getDoctorName());
        note.setHospitalId(dto.getHospitalId());
        note.setDoctorId(StringUtils.hasText(doctorId) ? doctorId : "0");

        PatientNote saved = patientNoteRepository.save(note);
        return dto;
    }

    @Transactional(readOnly = true)
    public PatientMedicalHistoryResponseDto getPatientFullHistory(String patientId, String hospitalId) {
        PatientMedicalHistoryResponseDto response = new PatientMedicalHistoryResponseDto();
        response.setPatientId(patientId);

        // Fetch past diagnoses
        response.setPastDiagnoses(medicalHistoryRepository.findByPatientIdAndHospitalId(patientId, hospitalId).stream()
            .map(h -> {
                MedicalHistoryDto dto = new MedicalHistoryDto();
                dto.setId(h.getId() != null ? h.getId().toString() : null);
                dto.setPatientId(h.getPatientId());
                dto.setTitle(h.getTitle());
                dto.setDescription(h.getDescription());
                dto.setPatientName(h.getPatientName());
                dto.setPatientAddress(h.getPatientAddress());
                dto.setPatientPhone(h.getPatientPhone());
                dto.setDate(h.getDate());
                dto.setDiagnosisCategory(h.getDiagnosisCategory());
                return dto;
            }).collect(Collectors.toList()));

        // Fetch clinical notes
        response.setClinicalNotes(patientNoteRepository.findByPatientId(patientId).stream()
            .filter(n -> hospitalId.equals(n.getHospitalId()))
            .map(n -> {
                PatientNoteDto dto = new PatientNoteDto();
                dto.setPatientId(n.getPatientId());
                dto.setTitle(n.getTitle());
                dto.setDescription(n.getDescription());
                dto.setPatientName(n.getPatientName());
                dto.setDate(n.getDate());
                dto.setStatus(n.getStatus());
                dto.setDoctorName(n.getDoctorName());
                dto.setDoctorId(n.getDoctorId());
                return dto;
            }).collect(Collectors.toList()));

        // Fetch vitals
        response.setVitals(patientTriageRepository.findByPatientAndHospitalId(patientId, hospitalId)
            .stream().map(PatientTriageResponseDto::from).collect(Collectors.toList()));

        // Fetch prescriptions
        response.setPrescriptions(prescriptionService.listByPatient(patientId, hospitalId));

        return response;
    }

    @Transactional(readOnly = true)
    public DoctorDhsReportResponseDto getDhsReport(
            String hospitalId,
            LocalDate dateFrom,
            LocalDate dateTo,
            String department
    ) {
        LocalDate normalizedFrom = dateFrom != null ? dateFrom : LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
        LocalDate normalizedTo = dateTo != null ? dateTo : normalizedFrom.withDayOfMonth(normalizedFrom.lengthOfMonth());

        if (normalizedTo.isBefore(normalizedFrom)) {
            throw new IllegalArgumentException("dateTo must be on or after dateFrom");
        }

        long fromEpoch = normalizedFrom.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
        long toEpoch = normalizedTo.plusDays(1).atStartOfDay().minusSeconds(1).toEpochSecond(ZoneOffset.UTC);
        String selectedDepartment = normalizeReportDepartment(department);
        List<String> ageBuckets = List.of("<1 year", "1 - 4 years", "5 - 14 years", "15 - 29 years", "30 - 64 years", ">=65 yr");

        List<MedicalHistory> histories = medicalHistoryRepository
                .findDiagnosisHistoryByHospitalIdAndDateRange(hospitalId, fromEpoch, toEpoch);

        List<String> patientIds = histories.stream()
                .map(MedicalHistory::getPatientId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();

        Map<String, Patient> patientsByPatientId = patientIds.isEmpty()
                ? Map.of()
                : patientRepository.findByHospitalIdAndPatientIdIn(hospitalId, patientIds).stream()
                .filter(patient -> StringUtils.hasText(patient.getPatientId()))
                .collect(Collectors.toMap(Patient::getPatientId, patient -> patient, (left, right) -> left));

        Set<String> patientIdSet = Set.copyOf(patientIds);
        Map<String, List<PatientVisit>> visitsByPatientId = patientVisitRepository
                .findVisitsByHospitalIdAndCreatedAtBetween(hospitalId, fromEpoch, toEpoch).stream()
                .filter(visit -> StringUtils.hasText(visit.getPatientId()) && patientIdSet.contains(visit.getPatientId()))
                .collect(Collectors.groupingBy(
                        PatientVisit::getPatientId,
                        LinkedHashMap::new,
                        Collectors.collectingAndThen(Collectors.toList(), visits ->
                                visits.stream()
                                        .sorted(Comparator.comparingLong(
                                                (PatientVisit visit) -> parseEpochSeconds(visit.getCreatedAt())
                                        ).reversed())
                                        .toList())
                ));

        Map<String, long[][]> diseaseMatrix = new LinkedHashMap<>();
        // Tracks the true patient count per disease regardless of demographic completeness
        Map<String, Long> diseaseTotalMap = new LinkedHashMap<>();
        long[] grandMaleCounts = new long[ageBuckets.size()];
        long[] grandFemaleCounts = new long[ageBuckets.size()];
        long grandTotalAll = 0L;

        for (MedicalHistory history : histories) {
            String resolvedDepartment = resolveDepartmentBucket(
                    history,
                    visitsByPatientId.getOrDefault(history.getPatientId(), List.of())
            );

            if (!selectedDepartment.equals(resolvedDepartment)) {
                continue;
            }

            // Always register the disease row — even when gender/age data is incomplete.
            // Previously this was placed after the gender/age guard, which caused the
            // entire disease to be invisible when patients lacked demographic data.
            String diagnosis = normalizeDiagnosis(history.getDiagnosisCategory());
            diseaseMatrix.computeIfAbsent(diagnosis, key -> new long[2][ageBuckets.size()]);
            diseaseTotalMap.merge(diagnosis, 1L, Long::sum);
            grandTotalAll++;

            Patient patient = patientsByPatientId.get(history.getPatientId());
            int genderIndex = resolveGenderIndex(patient);
            int ageIndex = resolveAgeBucketIndex(patient);
            if (genderIndex < 0 || ageIndex < 0) {
                // Demographic data missing — disease still shown, but age/sex buckets skipped.
                continue;
            }

            long[][] counts = diseaseMatrix.get(diagnosis);
            counts[genderIndex][ageIndex] += 1L;

            if (genderIndex == 0) {
                grandMaleCounts[ageIndex] += 1L;
            } else {
                grandFemaleCounts[ageIndex] += 1L;
            }
        }

        List<DoctorDhsReportResponseDto.DiseaseRowDto> diseaseRows = diseaseMatrix.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new DoctorDhsReportResponseDto.DiseaseRowDto(
                        entry.getKey(),
                        toLongList(entry.getValue()[0]),
                        toLongList(entry.getValue()[1]),
                        diseaseTotalMap.getOrDefault(entry.getKey(), 0L)
                ))
                .toList();

        return new DoctorDhsReportResponseDto(
                resolveOrganizationUnit(hospitalId),
                "16 - Disease Registration | Monthly",
                buildPeriodLabel(normalizedFrom, normalizedTo),
                normalizedFrom.toString(),
                normalizedTo.toString(),
                selectedDepartment,
                List.of("OPD", "EMERGENCY"),
                "Morbidity",
                ageBuckets,
                diseaseRows,
                toLongList(grandMaleCounts),
                toLongList(grandFemaleCounts),
                grandTotalAll
        );
    }

    private String normalizeDiagnosis(String diagnosisCategory) {
        if (!StringUtils.hasText(diagnosisCategory)) {
            return "Unspecified";
        }

        return diagnosisCategory.trim().replaceAll("\\s+", " ");
    }

    private String normalizeReportDepartment(String department) {
        if (!StringUtils.hasText(department)) {
            return "OPD";
        }

        String normalized = department.trim().toUpperCase(Locale.ROOT);
        return normalized.contains("EMER") ? "EMERGENCY" : "OPD";
    }

    private String resolveOrganizationUnit(String hospitalId) {
        return hospitalRepository.findByIonUserId(hospitalId)
                .map(Hospital::getName)
                .filter(StringUtils::hasText)
                .orElse(hospitalId);
    }

    private String buildPeriodLabel(LocalDate from, LocalDate to) {
        if (from.getYear() == to.getYear() && from.getMonth() == to.getMonth()) {
            return from.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH));
        }

        return from.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))
                + " - "
                + to.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH));
    }

    private String resolveDepartmentBucket(MedicalHistory history, List<PatientVisit> visits) {
        PatientVisit matchedVisit = matchVisitToHistory(history, visits);
        if (matchedVisit == null) {
            return "OPD";
        }

        if (StringUtils.hasText(matchedVisit.getVisitType())
                && matchedVisit.getVisitType().trim().equalsIgnoreCase("EMERGENCY")) {
            return "EMERGENCY";
        }

        String combinedDepartment = String.join(" ",
                safeLower(matchedVisit.getDepartmentName()),
                safeLower(matchedVisit.getAssignedDepartmentName()),
                safeLower(matchedVisit.getAssignedTo()));

        return combinedDepartment.contains("emergency") ? "EMERGENCY" : "OPD";
    }

    private PatientVisit matchVisitToHistory(MedicalHistory history, List<PatientVisit> visits) {
        if (visits == null || visits.isEmpty()) {
            return null;
        }

        long historyEpoch = parseEpochSeconds(history.getDate());
        if (historyEpoch <= 0L) {
            return visits.get(0);
        }

        return visits.stream()
                .min(Comparator.comparingLong(visit ->
                        Math.abs(parseEpochSeconds(visit.getCreatedAt()) - historyEpoch)))
                .orElse(visits.get(0));
    }

    private long parseEpochSeconds(String raw) {
        if (!StringUtils.hasText(raw)) {
            return -1L;
        }

        try {
            long value = Long.parseLong(raw.trim());
            return value > 100000000000L ? value / 1000L : value;
        } catch (NumberFormatException ignored) {
            return -1L;
        }
    }

    private String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private int resolveGenderIndex(Patient patient) {
        if (patient == null || !StringUtils.hasText(patient.getSex())) {
            return -1;
        }

        String normalized = patient.getSex().trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("m")) return 0;
        if (normalized.startsWith("f")) return 1;
        return -1;
    }

    private int resolveAgeBucketIndex(Patient patient) {
        Integer age = resolveAge(patient);
        if (age == null || age < 0) return -1;
        if (age < 1) return 0;
        if (age <= 4) return 1;
        if (age <= 14) return 2;
        if (age <= 29) return 3;
        if (age <= 64) return 4;
        return 5;
    }

    private List<Long> toLongList(long[] values) {
        List<Long> items = new java.util.ArrayList<>(values.length);
        for (long value : values) {
            items.add(value);
        }
        return items;
    }

    private long sumCounts(long[][] counts) {
        long total = 0L;
        for (long[] row : counts) {
            total += sumCounts(row);
        }
        return total;
    }

    private long sumCounts(long[] counts) {
        long total = 0L;
        for (long count : counts) {
            total += count;
        }
        return total;
    }

    private Integer resolveAge(Patient patient) {
        if (patient == null) return null;

        if (StringUtils.hasText(patient.getAge())) {
            try {
                return Integer.parseInt(patient.getAge().trim());
            } catch (NumberFormatException ignored) {
                // Fall through to birthdate parsing.
            }
        }

        if (!StringUtils.hasText(patient.getBirthdate())) {
            return null;
        }

        try {
            LocalDate birthdate = LocalDate.parse(
                    patient.getBirthdate().trim(),
                    DateTimeFormatter.ofPattern("dd-MM-yyyy")
            );
            return Period.between(birthdate, LocalDate.now(ZoneOffset.UTC)).getYears();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}
