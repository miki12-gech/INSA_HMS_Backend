package com.insa.hospital.config;

import com.insa.hospital.entity.Group;
import com.insa.hospital.entity.Hospital;
import com.insa.hospital.entity.Patient;
import com.insa.hospital.entity.User;
import com.insa.hospital.entity.UserGroup;
import com.insa.hospital.repository.GroupRepository;
import com.insa.hospital.repository.HospitalRepository;
import com.insa.hospital.repository.PatientRepository;
import com.insa.hospital.repository.UserGroupRepository;
import com.insa.hospital.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Ensures the employee self-service portal has the demo auth data it expects in local/dev.
 *
 * The frontend exposes an employee portal and the team tests it with:
 *   email    = employee@insa.com
 *   password = Employee@123
 *
 * Older database snapshots do not contain an Employee group or this login, which makes
 * /api/auth/login return 401 even though the feature exists in code.
 */
@Component
public class EmployeePortalBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(EmployeePortalBootstrap.class);

    private static final String EMPLOYEE_EMAIL = "employee@insa.com";
    private static final String EMPLOYEE_PASSWORD = "Employee@123";
    private static final String EMPLOYEE_ROLE = "Employee";
    private static final String DEFAULT_HOSPITAL_ADMIN_USER_ID = "2";

    private static final DateTimeFormatter PATIENT_ADD_DATE =
            DateTimeFormatter.ofPattern("MM/dd/yy");
    private static final DateTimeFormatter PATIENT_BIRTHDATE =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final GroupRepository groupRepository;
    private final HospitalRepository hospitalRepository;
    private final PatientRepository patientRepository;
    private final UserGroupRepository userGroupRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public EmployeePortalBootstrap(GroupRepository groupRepository,
                                   HospitalRepository hospitalRepository,
                                   PatientRepository patientRepository,
                                   UserGroupRepository userGroupRepository,
                                   UserRepository userRepository,
                                   PasswordEncoder passwordEncoder) {
        this.groupRepository = groupRepository;
        this.hospitalRepository = hospitalRepository;
        this.patientRepository = patientRepository;
        this.userGroupRepository = userGroupRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Group employeeGroup = ensureEmployeeGroup();
        Hospital hospital = resolveHospital();
        User employeeUser = ensureEmployeeUser(hospital);
        ensureEmployeeGroupMapping(employeeUser, employeeGroup);
        ensureEmployeePatientProfile(employeeUser, hospital);
    }

    private Group ensureEmployeeGroup() {
        return groupRepository.findAll().stream()
                .filter(group -> EMPLOYEE_ROLE.equalsIgnoreCase(group.getName()))
                .findFirst()
                .orElseGet(() -> {
                    Group group = new Group();
                    group.setName(EMPLOYEE_ROLE);
                    group.setDescription("Employee self-service portal");
                    Group saved = groupRepository.save(group);
                    log.info("Created missing auth group '{}'", EMPLOYEE_ROLE);
                    return saved;
                });
    }

    private Hospital resolveHospital() {
        return hospitalRepository.findByIonUserId(DEFAULT_HOSPITAL_ADMIN_USER_ID)
                .or(() -> hospitalRepository.findAll().stream().findFirst())
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot bootstrap employee portal: no hospital row exists."));
    }

    private User ensureEmployeeUser(Hospital hospital) {
        return userRepository.findByEmailIgnoreCase(EMPLOYEE_EMAIL)
                .map(existing -> {
                    boolean changed = false;

                    if (existing.getActive() == null || existing.getActive() != 1) {
                        existing.setActive(1);
                        changed = true;
                    }
                    if (existing.getHospitalIonId() == null || existing.getHospitalIonId().isBlank()) {
                        existing.setHospitalIonId(hospital.getIonUserId());
                        changed = true;
                    }

                    if (changed) {
                        User saved = userRepository.save(existing);
                        log.info("Updated existing demo employee login '{}'", saved.getEmail());
                        return saved;
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    long now = Instant.now().getEpochSecond();

                    User user = new User();
                    user.setIpAddress("127.0.0.1");
                    user.setUsername("INSA Employee");
                    user.setPassword(passwordEncoder.encode(EMPLOYEE_PASSWORD));
                    user.setEmail(EMPLOYEE_EMAIL);
                    user.setCreatedOn(now);
                    user.setLastLogin(now);
                    user.setActive(1);
                    user.setFirstName("INSA");
                    user.setLastName("Employee");
                    user.setCompany("INSA");
                    user.setPhone("0911000000");
                    user.setHospitalIonId(hospital.getIonUserId());

                    User saved = userRepository.save(user);
                    log.info("Created missing demo employee login '{}'", EMPLOYEE_EMAIL);
                    return saved;
                });
    }

    private void ensureEmployeeGroupMapping(User employeeUser, Group employeeGroup) {
        boolean alreadyMapped = userGroupRepository.findByUserId(employeeUser.getId())
                .map(mapping -> employeeGroup.getId().equals(mapping.getGroupId()))
                .orElse(false);

        if (alreadyMapped) {
            return;
        }

        userGroupRepository.findByUserId(employeeUser.getId()).ifPresent(userGroupRepository::delete);

        UserGroup mapping = new UserGroup();
        mapping.setUserId(employeeUser.getId());
        mapping.setGroupId(employeeGroup.getId());
        userGroupRepository.save(mapping);
        log.info("Linked '{}' to auth group '{}'", EMPLOYEE_EMAIL, employeeGroup.getName());
    }

    private void ensureEmployeePatientProfile(User employeeUser, Hospital hospital) {
        if (patientRepository.findByIonUserId(String.valueOf(employeeUser.getId())).isPresent()) {
            return;
        }

        LocalDate today = LocalDate.now(ZoneId.systemDefault());

        Patient patient = new Patient();
        patient.setImgUrl("default-image.png");
        patient.setName("INSA Employee");
        patient.setEmail(EMPLOYEE_EMAIL);
        patient.setPhone(employeeUser.getPhone());
        patient.setAddress("INSA");
        patient.setSex("Male");
        patient.setBirthdate(LocalDate.of(1995, 1, 1).format(PATIENT_BIRTHDATE));
        patient.setAge(String.valueOf(today.getYear() - 1995));
        patient.setBloodgroup("O+");
        patient.setIonUserId(String.valueOf(employeeUser.getId()));
        patient.setPatientId(nextPatientId());
        patient.setAddDate(today.format(PATIENT_ADD_DATE));
        patient.setRegistrationTime(String.valueOf(Instant.now().getEpochSecond()));
        patient.setHowAdded("employee_portal_bootstrap");
        patient.setHospitalId(String.valueOf(hospital.getId()));
        patient.setMembershiptype("member");
        patient.setAllergynote("");

        patientRepository.save(patient);
        log.info("Created patient profile for demo employee '{}'", EMPLOYEE_EMAIL);
    }

    private String nextPatientId() {
        String candidate;
        do {
            candidate = String.valueOf(100000 + random.nextInt(900000));
        } while (patientRepository.existsByPatientId(candidate));
        return candidate;
    }
}
