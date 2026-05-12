package com.insa.hospital.service;

import com.insa.hospital.dto.AdminUserCreateRequestDto;
import com.insa.hospital.dto.AdminUserDefaultPasswordDto;
import com.insa.hospital.dto.AdminUserPasswordResetDto;
import com.insa.hospital.dto.AdminUserResponseDto;
import com.insa.hospital.dto.AdminUserStatusUpdateDto;
import com.insa.hospital.entity.Department;
import com.insa.hospital.entity.Group;
import com.insa.hospital.entity.Hospital;
import com.insa.hospital.entity.Settings;
import com.insa.hospital.entity.User;
import com.insa.hospital.entity.UserGroup;
import com.insa.hospital.exception.ResourceNotFoundException;
import com.insa.hospital.repository.DepartmentRepository;
import com.insa.hospital.repository.GroupRepository;
import com.insa.hospital.repository.HospitalRepository;
import com.insa.hospital.repository.SettingsRepository;
import com.insa.hospital.repository.UserGroupRepository;
import com.insa.hospital.repository.UserRepository;
import com.insa.hospital.util.StaffEmailPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Superadmin IAM service for managing legacy staff accounts.
 *
 * Persistence rules:
 * - Credentials live in ion_auth tables: users + users_groups.
 * - Department linkage is stored in users.company because the legacy users table
 *   has no dedicated department FK column.
 * - Role profile rows are inserted into the corresponding legacy role table when
 *   that table exists (doctor, nurse, pharmacist, laboratorist, accountant, receptionist).
 */
@Service
@RequiredArgsConstructor
public class UserManagementService {

    private static final String DEFAULT_PASSWORD = "12345678";

    private static final Set<String> ALLOWED_GROUPS = Set.of(
            "Doctor",
            "Nurse",
            "Pharmacist",
            "Laboratorist",
            "Receptionist",
            "Accountant",
            "admin"
    );

    private final UserRepository userRepository;
    private final UserGroupRepository userGroupRepository;
    private final GroupRepository groupRepository;
    private final DepartmentRepository departmentRepository;
    private final HospitalRepository hospitalRepository;
    private final SettingsRepository settingsRepository;
    private final TenantAccessService tenantAccessService;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public List<AdminUserResponseDto> listUsers(String hospitalId, String roleFilter) {
        String normalizedRole = normalizeRole(roleFilter, true);
        List<String> hospitalScopeIds = tenantAccessService.isSuperAdmin()
                ? List.of()
                : resolveHospitalScopeIds(hospitalId);

        StringBuilder sql = new StringBuilder("""
            SELECT
                u.id,
                u.username,
                u.email,
                u.phone,
                u.active,
                u.created_on,
                u.hospital_ion_id,
                u.company,
                g.name AS group_name
            FROM users u
            JOIN users_groups ug ON ug.user_id = u.id
            JOIN groups g ON g.id = ug.group_id
            WHERE LOWER(g.name) IN ('doctor', 'nurse', 'pharmacist', 'laboratorist', 'receptionist', 'accountant', 'admin')
            ORDER BY u.created_on DESC, u.id DESC
            """);

        List<Object> params = new ArrayList<>();

        if (!hospitalScopeIds.isEmpty()) {
            int orderByIndex = sql.indexOf("ORDER BY");
            sql.insert(orderByIndex, "  AND u.hospital_ion_id IN (" + repeatPlaceholders(hospitalScopeIds.size()) + ")\n");
            params.addAll(hospitalScopeIds);
        }

        if (StringUtils.hasText(normalizedRole)) {
            int orderByIndex = sql.indexOf("ORDER BY");
            sql.insert(orderByIndex, "  AND LOWER(g.name) = LOWER(?)\n");
            params.add(normalizedRole);
        }

        List<AdminUserRow> rows = jdbcTemplate.query(
                sql.toString(),
                (rs, rowNum) -> mapUserRow(rs),
                params.toArray()
        );

        Map<Integer, Department> departmentsById = loadDepartmentsById(hospitalId);
        Map<String, String> hospitalNamesByScopeId = loadHospitalNamesByScopeId();

        return rows.stream()
                .map(row -> toResponse(row, departmentsById, hospitalNamesByScopeId))
                .toList();
    }

    @Transactional
    public AdminUserResponseDto createUser(String hospitalId, AdminUserCreateRequestDto dto) {
        String normalizedRole = normalizeRole(dto.role(), false);
        String normalizedEmail = StaffEmailPolicy.normalizeStaffEmail(dto.email());
        String requestedHospitalId = StringUtils.hasText(dto.hospitalId())
                ? normalizeHospitalScopeId(dto.hospitalId())
                : normalizeHospitalScopeId(hospitalId);
        Department department = resolveDepartment(dto.departmentId(), requestedHospitalId);
        String effectiveHospitalId = StringUtils.hasText(requestedHospitalId)
                ? requestedHospitalId
                : normalizeHospitalScopeId(department.getHospitalId());

        if (!StringUtils.hasText(effectiveHospitalId)) {
            throw new IllegalArgumentException("Unable to resolve hospitalId for the selected department.");
        }

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("Email is already registered.");
        }

        Group group = groupRepository.findByName(normalizedRole)
                .orElseThrow(() -> new ResourceNotFoundException("Group", "name", normalizedRole));

        NameParts nameParts = splitName(dto.name());
        long epochSeconds = Instant.now().getEpochSecond();

        User user = new User();
        user.setIpAddress("127.0.0.1");
        user.setUsername(dto.name().trim());
        user.setPassword(passwordEncoder.encode(getConfiguredDefaultPassword()));
        user.setSalt(null);
        user.setEmail(normalizedEmail);
        user.setActivationCode(null);
        user.setForgottenPasswordCode(null);
        user.setForgottenPasswordTime(null);
        user.setRememberCode(null);
        user.setCreatedOn(epochSeconds);
        user.setLastLogin(null);
        user.setActive(1);
        user.setFirstName(nameParts.firstName());
        user.setLastName(nameParts.lastName());
        user.setCompany(String.valueOf(department.getId()));
        user.setPhone(dto.phone().trim());
        user.setHospitalIonId(effectiveHospitalId);

        User savedUser = userRepository.save(user);

        UserGroup userGroup = new UserGroup();
        userGroup.setUserId(savedUser.getId());
        userGroup.setGroupId(group.getId());
        userGroupRepository.save(userGroup);

        createLegacyStaffProfile(savedUser, normalizedRole, department, effectiveHospitalId);

        return new AdminUserResponseDto(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getPhone(),
                displayRole(normalizedRole),
                savedUser.getActive() != null && savedUser.getActive() == 1,
                effectiveHospitalId,
                resolveHospitalName(effectiveHospitalId, loadHospitalNamesByScopeId()),
                department.getId(),
                department.getName(),
                savedUser.getCreatedOn()
        );
    }

    @Transactional
    public AdminUserResponseDto updateStatus(String hospitalId, Long userId, AdminUserStatusUpdateDto dto) {
        User user = findManagedUser(userId, hospitalId);
        String groupName = resolveManagedGroupName(user.getId());
        Map<Integer, Department> departmentsById = loadDepartmentsById(hospitalId);

        user.setActive(Boolean.TRUE.equals(dto.active()) ? 1 : 0);
        User saved = userRepository.save(user);

        Integer departmentId = parseDepartmentId(saved.getCompany());
        Department department = departmentId != null ? departmentsById.get(departmentId) : null;

        return new AdminUserResponseDto(
                saved.getId(),
                saved.getUsername(),
                saved.getEmail(),
                saved.getPhone(),
                displayRole(groupName),
                saved.getActive() != null && saved.getActive() == 1,
                saved.getHospitalIonId(),
                resolveHospitalName(saved.getHospitalIonId(), loadHospitalNamesByScopeId()),
                departmentId,
                department != null ? department.getName() : null,
                saved.getCreatedOn()
        );
    }

    @Transactional
    public void resetPassword(String hospitalId, Long userId, AdminUserPasswordResetDto dto) {
        User user = findManagedUser(userId, hospitalId);

        String newPassword = StringUtils.hasText(dto.newPassword())
                ? dto.newPassword().trim()
                : getConfiguredDefaultPassword();

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setForgottenPasswordCode(null);
        user.setForgottenPasswordTime(null);
        user.setRememberCode(null);

        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public AdminUserDefaultPasswordDto getDefaultPassword() {
        return new AdminUserDefaultPasswordDto(getConfiguredDefaultPassword());
    }

    @Transactional
    public AdminUserDefaultPasswordDto updateDefaultPassword(AdminUserDefaultPasswordDto dto) {
        String sanitizedPassword = dto.password().trim();
        Settings settings = findOrCreateDefaultPasswordSettings();
        settings.setCodecPurchaseCode(sanitizedPassword);
        settingsRepository.save(settings);

        return new AdminUserDefaultPasswordDto(sanitizedPassword);
    }

    private User findManagedUser(Long userId, String hospitalId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!tenantAccessService.isSuperAdmin()
                && StringUtils.hasText(hospitalId)
                && !resolveHospitalScopeIds(hospitalId).contains(user.getHospitalIonId())) {
            throw new ResourceNotFoundException("User", "id", userId);
        }

        String groupName = resolveManagedGroupName(userId);
        if (!ALLOWED_GROUPS.contains(groupName)) {
            throw new IllegalArgumentException("Only staff accounts can be managed from this endpoint.");
        }

        return user;
    }

    private String resolveManagedGroupName(Long userId) {
        String groupName = userGroupRepository.findGroupNameByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserGroup", "userId", userId));

        if (!ALLOWED_GROUPS.contains(groupName)) {
            throw new IllegalArgumentException("Unsupported staff role: " + groupName);
        }

        return groupName;
    }

    private Department resolveDepartment(Integer departmentId, String hospitalId) {
        return departmentRepository.findById(departmentId)
                .filter(department -> !StringUtils.hasText(hospitalId)
                        || resolveHospitalScopeIds(hospitalId).contains(department.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", departmentId));
    }

    private Map<Integer, Department> loadDepartmentsById(String hospitalId) {
        List<String> hospitalScopeIds = tenantAccessService.isSuperAdmin()
                ? List.of()
                : resolveHospitalScopeIds(hospitalId);
        List<Department> departments = !hospitalScopeIds.isEmpty()
                ? departmentRepository.findByHospitalIdIn(hospitalScopeIds)
                : departmentRepository.findAll();

        Map<Integer, Department> result = new LinkedHashMap<>();
        for (Department department : departments) {
            result.put(department.getId(), department);
        }
        return result;
    }

    private AdminUserRow mapUserRow(ResultSet rs) throws SQLException {
        return new AdminUserRow(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getInt("active"),
                rs.getLong("created_on"),
                rs.getString("hospital_ion_id"),
                rs.getString("company"),
                rs.getString("group_name")
        );
    }

    private AdminUserResponseDto toResponse(AdminUserRow row,
                                            Map<Integer, Department> departmentsById,
                                            Map<String, String> hospitalNamesByScopeId) {
        Integer departmentId = parseDepartmentId(row.departmentKey());
        Department department = departmentId != null ? departmentsById.get(departmentId) : null;
        String hospitalId = normalizeHospitalScopeId(row.hospitalId());

        return new AdminUserResponseDto(
                row.id(),
                row.name(),
                row.email(),
                row.phone(),
                displayRole(row.groupName()),
                row.active() == 1,
                hospitalId,
                resolveHospitalName(hospitalId, hospitalNamesByScopeId),
                departmentId,
                department != null ? department.getName() : null,
                row.createdOn()
        );
    }

    private Map<String, String> loadHospitalNamesByScopeId() {
        Map<String, String> result = new LinkedHashMap<>();

        for (Hospital hospital : hospitalRepository.findAll()) {
            String name = StringUtils.hasText(hospital.getName()) ? hospital.getName().trim() : null;
            if (!StringUtils.hasText(name)) {
                continue;
            }

            if (StringUtils.hasText(hospital.getIonUserId())) {
                result.put(hospital.getIonUserId().trim(), name);
            }
            result.put(String.valueOf(hospital.getId()), name);
        }

        return result;
    }

    private String resolveHospitalName(String hospitalId, Map<String, String> hospitalNamesByScopeId) {
        if (!StringUtils.hasText(hospitalId)) {
            return "Unassigned Hospital";
        }

        String normalizedHospitalId = normalizeHospitalScopeId(hospitalId);
        String hospitalName = hospitalNamesByScopeId.get(normalizedHospitalId);
        if (StringUtils.hasText(hospitalName)) {
            return hospitalName;
        }

        hospitalName = hospitalNamesByScopeId.get(hospitalId.trim());
        if (StringUtils.hasText(hospitalName)) {
            return hospitalName;
        }

        return "Hospital #" + hospitalId.trim();
    }

    private Integer parseDepartmentId(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }
        try {
            return Integer.parseInt(rawValue.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String normalizeRole(String role, boolean allowNull) {
        if (!StringUtils.hasText(role)) {
            if (allowNull) {
                return null;
            }
            throw new IllegalArgumentException("Role is required.");
        }

        return switch (role.trim().toLowerCase(Locale.ROOT)) {
            case "doctor" -> "Doctor";
            case "nurse" -> "Nurse";
            case "pharmacist" -> "Pharmacist";
            case "laboratorist" -> "Laboratorist";
            case "receptionist" -> "Receptionist";
            case "economist", "accountant" -> "Accountant";
            case "admin" -> "admin";
            default -> throw new IllegalArgumentException("Unsupported staff role: " + role);
        };
    }

    private String displayRole(String normalizedRole) {
        return switch (normalizedRole) {
            case "Accountant" -> "Economist";
            case "admin" -> "Admin";
            default -> normalizedRole;
        };
    }

    private List<String> resolveHospitalScopeIds(String hospitalId) {
        LinkedHashSet<String> scopeIds = new LinkedHashSet<>();
        String canonicalHospitalId = normalizeHospitalScopeId(hospitalId);

        if (StringUtils.hasText(canonicalHospitalId)) {
            scopeIds.add(canonicalHospitalId);

            hospitalRepository.findByIonUserId(canonicalHospitalId)
                    .map(Hospital::getId)
                    .map(String::valueOf)
                    .ifPresent(scopeIds::add);
        }

        if (StringUtils.hasText(hospitalId)) {
            scopeIds.add(hospitalId.trim());
        }

        return new ArrayList<>(scopeIds);
    }

    private String normalizeHospitalScopeId(String hospitalId) {
        if (!StringUtils.hasText(hospitalId)) {
            return hospitalId;
        }

        String trimmed = hospitalId.trim();
        if (hospitalRepository.findByIonUserId(trimmed).isPresent()) {
            return trimmed;
        }

        try {
            return hospitalRepository.findById(Integer.parseInt(trimmed))
                    .map(Hospital::getIonUserId)
                    .filter(StringUtils::hasText)
                    .orElse(trimmed);
        } catch (NumberFormatException ignored) {
            return trimmed;
        }
    }

    private String getConfiguredDefaultPassword() {
        for (String settingsHospitalId : resolveDefaultPasswordSettingsScopeIds()) {
            if (!StringUtils.hasText(settingsHospitalId)) {
                continue;
            }

            String configuredPassword = settingsRepository.findByHospitalId(settingsHospitalId)
                    .map(Settings::getCodecPurchaseCode)
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .orElse(null);
            if (StringUtils.hasText(configuredPassword)) {
                return configuredPassword;
            }
        }

        return DEFAULT_PASSWORD;
    }

    private Settings findOrCreateDefaultPasswordSettings() {
        for (String settingsHospitalId : resolveDefaultPasswordSettingsScopeIds()) {
            if (!StringUtils.hasText(settingsHospitalId)) {
                continue;
            }

            Settings existing = settingsRepository.findByHospitalId(settingsHospitalId).orElse(null);
            if (existing != null) {
                return existing;
            }
        }

        Settings settings = new Settings();
        String fallbackHospitalId = resolveDefaultPasswordSettingsScopeIds().stream()
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(tenantAccessService.getDefaultMainHospitalId());
        settings.setHospitalId(fallbackHospitalId);

        hospitalRepository.findByIonUserId(tenantAccessService.getDefaultMainHospitalId())
                .ifPresent(hospital -> {
                    settings.setTitle(hospital.getName());
                    settings.setEmail(hospital.getEmail());
                    settings.setAddress(hospital.getAddress());
                    settings.setPhone(hospital.getPhone());
                });

        return settings;
    }

    private List<String> resolveDefaultPasswordSettingsScopeIds() {
        LinkedHashSet<String> scopeIds = new LinkedHashSet<>();
        String defaultMainHospitalId = tenantAccessService.getDefaultMainHospitalId();

        if (StringUtils.hasText(defaultMainHospitalId)) {
            scopeIds.add(defaultMainHospitalId);
            hospitalRepository.findByIonUserId(defaultMainHospitalId)
                    .map(Hospital::getId)
                    .map(String::valueOf)
                    .ifPresent(scopeIds::add);
        }

        return new ArrayList<>(scopeIds);
    }

    private String repeatPlaceholders(int count) {
        return String.join(", ", Collections.nCopies(count, "?"));
    }

    private NameParts splitName(String fullName) {
        String trimmed = fullName == null ? "" : fullName.trim();
        if (trimmed.isEmpty()) {
            return new NameParts("", "");
        }

        String[] parts = trimmed.split("\\s+", 2);
        String firstName = truncate(parts[0], 50);
        String lastName = parts.length > 1 ? truncate(parts[1], 50) : "";
        return new NameParts(firstName, lastName);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private void createLegacyStaffProfile(User user,
                                          String normalizedRole,
                                          Department department,
                                          String hospitalId) {
        String userId = String.valueOf(user.getId());

        switch (normalizedRole) {
            case "Doctor" -> jdbcTemplate.update(
                    """
                    INSERT INTO doctor
                        (img_url, name, email, address, phone, department, profile, x, y, ion_user_id, hospital_id)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    "",
                    truncate(user.getUsername(), 100),
                    truncate(user.getEmail(), 100),
                    "",
                    truncate(user.getPhone(), 100),
                    truncate(department.getName(), 100),
                    "",
                    "",
                    "",
                    userId,
                    hospitalId
            );
            case "Nurse" -> jdbcTemplate.update(
                    """
                    INSERT INTO nurse
                        (img_url, name, email, address, phone, x, y, z, ion_user_id, hospital_id)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    "",
                    truncate(user.getUsername(), 100),
                    truncate(user.getEmail(), 100),
                    "",
                    truncate(user.getPhone(), 100),
                    "",
                    "",
                    "",
                    userId,
                    hospitalId
            );
            case "Pharmacist" -> jdbcTemplate.update(
                    """
                    INSERT INTO pharmacist
                        (img_url, name, email, address, phone, x, y, ion_user_id, hospital_id)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    "",
                    truncate(user.getUsername(), 100),
                    truncate(user.getEmail(), 100),
                    "",
                    truncate(user.getPhone(), 100),
                    "",
                    "",
                    userId,
                    hospitalId
            );
            case "Laboratorist" -> jdbcTemplate.update(
                    """
                    INSERT INTO laboratorist
                        (img_url, name, email, address, phone, x, y, ion_user_id, hospital_id)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    "",
                    truncate(user.getUsername(), 100),
                    truncate(user.getEmail(), 100),
                    "",
                    truncate(user.getPhone(), 100),
                    "",
                    "",
                    userId,
                    hospitalId
            );
            case "Accountant" -> jdbcTemplate.update(
                    """
                    INSERT INTO accountant
                        (img_url, name, email, address, phone, x, ion_user_id, hospital_id)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    "",
                    truncate(user.getUsername(), 100),
                    truncate(user.getEmail(), 100),
                    "",
                    truncate(user.getPhone(), 100),
                    "",
                    userId,
                    hospitalId
            );
            case "Receptionist" -> jdbcTemplate.update(
                    """
                    INSERT INTO receptionist
                        (img_url, name, email, address, phone, x, ion_user_id, hospital_id)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    "",
                    truncate(user.getUsername(), 100),
                    truncate(user.getEmail(), 100),
                    "",
                    truncate(user.getPhone(), 100),
                    "",
                    userId,
                    hospitalId
            );
            case "admin" -> {
                // Hospital admin users authenticate through ion_auth only.
                // There is no separate legacy `admin` table to insert into.
            }
            default -> throw new IllegalArgumentException("Unsupported staff role: " + normalizedRole);
        }
    }

    private record NameParts(String firstName, String lastName) {}

    private record AdminUserRow(
            Long id,
            String name,
            String email,
            String phone,
            Integer active,
            Long createdOn,
            String hospitalId,
            String departmentKey,
            String groupName
    ) {}
}
