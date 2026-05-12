package com.insa.hospital.service;

import com.insa.hospital.dto.EmployeeRegistrationRequest;
import com.insa.hospital.dto.EmployeeRegistrationResponse;
import com.insa.hospital.dto.PendingEmployeeResponse;
import com.insa.hospital.entity.Group;
import com.insa.hospital.entity.Hospital;
import com.insa.hospital.entity.Patient;
import com.insa.hospital.entity.User;
import com.insa.hospital.entity.UserGroup;
import com.insa.hospital.exception.BadRequestException;
import com.insa.hospital.exception.ResourceNotFoundException;
import com.insa.hospital.repository.GroupRepository;
import com.insa.hospital.repository.HospitalRepository;
import com.insa.hospital.repository.PatientRepository;
import com.insa.hospital.repository.UserGroupRepository;
import com.insa.hospital.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Handles the manual approval workflow for self-registered employee accounts.
 *
 * POC note:
 * The legacy ion_auth schema has no dedicated INSA ID field, so the submitted
 * ID card number is stored in users.company for Employee accounts only.
 */
@Service
@Transactional
public class EmployeeApprovalService {

    private static final String EMPLOYEE_ROLE = "Employee";
    private static final String DEFAULT_HOSPITAL_ADMIN_USER_ID = "2";
    private static final String PENDING_APPROVAL_MARKER = "PENDING_APPROVAL";

    private static final DateTimeFormatter PATIENT_ADD_DATE =
            DateTimeFormatter.ofPattern("MM/dd/yy");

    private final UserRepository userRepository;
    private final UserGroupRepository userGroupRepository;
    private final GroupRepository groupRepository;
    private final HospitalRepository hospitalRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public EmployeeApprovalService(UserRepository userRepository,
                                   UserGroupRepository userGroupRepository,
                                   GroupRepository groupRepository,
                                   HospitalRepository hospitalRepository,
                                   PatientRepository patientRepository,
                                   PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userGroupRepository = userGroupRepository;
        this.groupRepository = groupRepository;
        this.hospitalRepository = hospitalRepository;
        this.patientRepository = patientRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public EmployeeRegistrationResponse registerPendingEmployee(
            EmployeeRegistrationRequest request,
            String remoteAddress
    ) {
        String fullName = request.name().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String phone = request.phone().trim();
        String insaIdCardNumber = request.insaIdCardNumber().trim();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("An account with this email already exists or is pending approval.");
        }

        if (userRepository.existsByCompanyAndGroupName(insaIdCardNumber, EMPLOYEE_ROLE)) {
            throw new BadRequestException("An account with this INSA ID is already registered or pending approval.");
        }

        Group employeeGroup = ensureEmployeeGroup();
        Hospital hospital = resolveDefaultHospital();
        long now = Instant.now().getEpochSecond();

        String[] nameParts = splitName(fullName);

        User user = new User();
        user.setIpAddress(normaliseIpAddress(remoteAddress));
        user.setUsername(fullName);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEmail(email);
        user.setCreatedOn(now);
        user.setLastLogin(null);
        user.setActive(0);
        user.setFirstName(nameParts[0]);
        user.setLastName(nameParts[1]);
        user.setCompany(insaIdCardNumber);
        user.setPhone(phone);
        user.setHospitalIonId(hospital.getIonUserId());
        user.setActivationCode(PENDING_APPROVAL_MARKER);

        User saved = userRepository.save(user);

        UserGroup mapping = new UserGroup();
        mapping.setUserId(saved.getId());
        mapping.setGroupId(employeeGroup.getId());
        userGroupRepository.save(mapping);

        return new EmployeeRegistrationResponse(
                saved.getId(),
                "PENDING",
                "Registration successful! Your account is pending administrator approval."
        );
    }

    @Transactional(readOnly = true)
    public List<PendingEmployeeResponse> getPendingEmployees(
            String hospitalIonId,
            String requesterUserId,
            boolean includeAllHospitals
    ) {
        String effectiveHospitalIonId = resolveEffectiveHospitalScope(
                hospitalIonId,
                requesterUserId,
                includeAllHospitals
        );

        return userRepository.findByGroupNameAndActive(
                        EMPLOYEE_ROLE,
                        0,
                        effectiveHospitalIonId
                )
                .stream()
                .map(this::toPendingEmployeeResponse)
                .toList();
    }

    public PendingEmployeeResponse approveEmployee(
            Long userId,
            String requesterHospitalIonId,
            String requesterUserId,
            boolean includeAllHospitals
    ) {
        User user = getScopedPendingEmployee(
                userId,
                requesterHospitalIonId,
                requesterUserId,
                includeAllHospitals
        );

        user.setActive(1);
        user.setActivationCode(null);
        User saved = userRepository.save(user);

        ensureEmployeePatientProfile(saved);

        return toPendingEmployeeResponse(saved);
    }

    public void rejectEmployee(
            Long userId,
            String requesterHospitalIonId,
            String requesterUserId,
            boolean includeAllHospitals
    ) {
        User user = getScopedPendingEmployee(
                userId,
                requesterHospitalIonId,
                requesterUserId,
                includeAllHospitals
        );

        patientRepository.findByIonUserId(String.valueOf(user.getId()))
                .ifPresent(patientRepository::delete);

        userGroupRepository.findByUserId(user.getId())
                .ifPresent(userGroupRepository::delete);

        userRepository.delete(user);
    }

    private User getScopedPendingEmployee(
            Long userId,
            String requesterHospitalIonId,
            String requesterUserId,
            boolean includeAllHospitals
    ) {
        User user = userRepository.findByIdAndGroupName(userId, EMPLOYEE_ROLE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pending employee account not found with id: " + userId));

        String effectiveHospitalIonId = resolveEffectiveHospitalScope(
                requesterHospitalIonId,
                requesterUserId,
                includeAllHospitals
        );

        if (!includeAllHospitals
                && effectiveHospitalIonId != null
                && !effectiveHospitalIonId.equals(user.getHospitalIonId())) {
            throw new AccessDeniedException("You do not have access to manage this employee request.");
        }

        if (user.getActive() != null && user.getActive() == 1) {
            throw new BadRequestException("Employee account is already approved.");
        }

        return user;
    }

    private String resolveEffectiveHospitalScope(
            String requesterHospitalIonId,
            String requesterUserId,
            boolean includeAllHospitals
    ) {
        if (includeAllHospitals) {
            return null;
        }

        if (requesterHospitalIonId != null && !requesterHospitalIonId.isBlank()) {
            return requesterHospitalIonId;
        }

        if (requesterUserId != null && !requesterUserId.isBlank()) {
            boolean ownsHospital = hospitalRepository.findByIonUserId(requesterUserId).isPresent();
            if (ownsHospital) {
                return requesterUserId;
            }
        }

        return null;
    }

    private PendingEmployeeResponse toPendingEmployeeResponse(User user) {
        String fullName = buildDisplayName(user);
        return new PendingEmployeeResponse(
                user.getId(),
                fullName,
                user.getEmail(),
                user.getPhone(),
                user.getCompany(),
                user.getCreatedOn()
        );
    }

    private String buildDisplayName(User user) {
        String firstName = safe(user.getFirstName());
        String lastName = safe(user.getLastName());
        String combined = (firstName + " " + lastName).trim();

        if (!combined.isBlank()) {
            return combined;
        }
        if (!safe(user.getUsername()).isBlank()) {
            return user.getUsername().trim();
        }
        return user.getEmail();
    }

    private Group ensureEmployeeGroup() {
        return groupRepository.findByName(EMPLOYEE_ROLE)
                .or(() -> groupRepository.findAll().stream()
                        .filter(group -> EMPLOYEE_ROLE.equalsIgnoreCase(group.getName()))
                        .findFirst())
                .orElseGet(() -> {
                    Group group = new Group();
                    group.setName(EMPLOYEE_ROLE);
                    group.setDescription("Employee self-service portal");
                    return groupRepository.save(group);
                });
    }

    private Hospital resolveDefaultHospital() {
        return hospitalRepository.findByIonUserId(DEFAULT_HOSPITAL_ADMIN_USER_ID)
                .or(() -> hospitalRepository.findAll().stream().findFirst())
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot register employee account: no hospital row exists."));
    }

    private Hospital resolveHospitalForUser(User user) {
        if (user.getHospitalIonId() != null && !user.getHospitalIonId().isBlank()) {
            return hospitalRepository.findByIonUserId(user.getHospitalIonId())
                    .orElseGet(this::resolveDefaultHospital);
        }
        return resolveDefaultHospital();
    }

    private void ensureEmployeePatientProfile(User user) {
        if (patientRepository.findByIonUserId(String.valueOf(user.getId())).isPresent()) {
            return;
        }

        Hospital hospital = resolveHospitalForUser(user);
        LocalDate today = LocalDate.now(ZoneId.systemDefault());

        Patient patient = new Patient();
        patient.setImgUrl("default-image.png");
        patient.setName(buildDisplayName(user));
        patient.setEmail(user.getEmail());
        patient.setPhone(user.getPhone());
        patient.setAddress("INSA");
        patient.setSex("");
        patient.setBirthdate("");
        patient.setAge("");
        patient.setBloodgroup("");
        patient.setIonUserId(String.valueOf(user.getId()));
        patient.setPatientId(nextPatientId());
        patient.setAddDate(today.format(PATIENT_ADD_DATE));
        patient.setRegistrationTime(String.valueOf(Instant.now().getEpochSecond()));
        patient.setHowAdded("employee_portal_approval");
        patient.setHospitalId(String.valueOf(hospital.getId()));
        patient.setMembershiptype("member");
        patient.setAllergynote("");

        patientRepository.save(patient);
    }

    private String nextPatientId() {
        String candidate;
        do {
            candidate = String.valueOf(100000 + random.nextInt(900000));
        } while (patientRepository.existsByPatientId(candidate));
        return candidate;
    }

    private String[] splitName(String fullName) {
        String[] parts = fullName.trim().split("\\s+", 2);
        String firstName = parts[0];
        String lastName = parts.length > 1 ? parts[1] : "Employee";
        return new String[]{firstName, lastName};
    }

    private String normaliseIpAddress(String remoteAddress) {
        if (remoteAddress == null || remoteAddress.isBlank()) {
            return "127.0.0.1";
        }
        if (remoteAddress.contains(":") || remoteAddress.length() > 15) {
            return "127.0.0.1";
        }
        return remoteAddress;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
