package com.insa.hospital.service;

import com.insa.hospital.dto.HospitalRequestDto;
import com.insa.hospital.dto.HospitalResponseDto;
import com.insa.hospital.entity.BloodBank;
import com.insa.hospital.entity.EmailSettings;
import com.insa.hospital.entity.Group;
import com.insa.hospital.entity.Hospital;
import com.insa.hospital.entity.PaymentGateway;
import com.insa.hospital.entity.Settings;
import com.insa.hospital.entity.SmsSettings;
import com.insa.hospital.entity.User;
import com.insa.hospital.entity.UserGroup;
import com.insa.hospital.repository.BloodBankRepository;
import com.insa.hospital.repository.EmailSettingsRepository;
import com.insa.hospital.repository.GroupRepository;
import com.insa.hospital.repository.HospitalRepository;
import com.insa.hospital.repository.PaymentGatewayRepository;
import com.insa.hospital.repository.SettingsRepository;
import com.insa.hospital.repository.SmsSettingsRepository;
import com.insa.hospital.repository.UserGroupRepository;
import com.insa.hospital.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HospitalService {

    private static final String DEFAULT_BRANCH_ADMIN_PASSWORD = "123456";
    private static final String USAGE_STATUS_ACTIVE = "Active";
    private static final String USAGE_STATUS_STOPPED = "Stopped";

    private final HospitalRepository hospitalRepository;
    private final UserRepository userRepository;
    private final UserGroupRepository userGroupRepository;
    private final GroupRepository groupRepository;
    private final SettingsRepository settingsRepository;
    private final BloodBankRepository bloodBankRepository;
    private final SmsSettingsRepository smsSettingsRepository;
    private final PaymentGatewayRepository paymentGatewayRepository;
    private final EmailSettingsRepository emailSettingsRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<Hospital> getAllHospitals() {
        return hospitalRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<HospitalResponseDto> getAllHospitalResponses() {
        return hospitalRepository.findAll().stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<Hospital> getHospitalById(Integer id) {
        return hospitalRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public HospitalResponseDto toResponseDto(Hospital hospital) {
        HospitalResponseDto response = new HospitalResponseDto(hospital);
        response.setUsageStatus(getHospitalUsageStatus(hospital));
        return response;
    }

    @Transactional
    public Hospital createHospital(HospitalRequestDto dto) {
        validateCreateRequest(dto);

        if (userRepository.existsByEmailIgnoreCase(dto.getEmail().trim())) {
            throw new IllegalArgumentException("Email is already registered.");
        }

        User adminUser = new User();
        adminUser.setIpAddress("127.0.0.1");
        adminUser.setUsername(dto.getName().trim());
        adminUser.setPassword(passwordEncoder.encode(resolveAdminPassword(dto)));
        adminUser.setSalt(null);
        adminUser.setEmail(dto.getEmail().trim().toLowerCase());
        adminUser.setActivationCode(null);
        adminUser.setForgottenPasswordCode(null);
        adminUser.setForgottenPasswordTime(null);
        adminUser.setRememberCode(null);
        adminUser.setCreatedOn(Instant.now().getEpochSecond());
        adminUser.setLastLogin(null);
        adminUser.setActive(1);
        adminUser.setFirstName(dto.getName().trim());
        adminUser.setLastName("");
        adminUser.setCompany("");
        adminUser.setPhone(trim(dto.getPhone()));
        adminUser.setHospitalIonId(null);
        adminUser = userRepository.save(adminUser);

        UserGroup adminUserGroup = new UserGroup();
        adminUserGroup.setUserId(adminUser.getId());
        adminUserGroup.setGroupId(resolveAdminGroup().getId());
        userGroupRepository.save(adminUserGroup);

        String tenantHospitalId = String.valueOf(adminUser.getId());

        Hospital hospital = new Hospital();
        hospital.setName(dto.getName().trim());
        hospital.setEmail(dto.getEmail().trim().toLowerCase());
        hospital.setPassword("");
        hospital.setAddress(trim(dto.getAddress()));
        hospital.setPhone(trim(dto.getPhone()));
        hospital.setHospitalPackage(trim(dto.getPackageId()));
        hospital.setPLimit(trim(dto.getP_limit()));
        hospital.setDLimit(trim(dto.getD_limit()));
        hospital.setModule(dto.getModule() != null ? String.join(",", dto.getModule()) : "");
        hospital.setIonUserId(tenantHospitalId);
        hospital = hospitalRepository.save(hospital);

        String hospitalId = String.valueOf(hospital.getId());

        adminUser.setHospitalIonId(tenantHospitalId);
        userRepository.save(adminUser);

        seedHospitalDefaults(dto, hospitalId, adminUser.getId());
        return hospital;
    }

    @Transactional
    public Hospital updateHospital(Integer id, HospitalRequestDto dto) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hospital not found"));

        hospital.setName(dto.getName());
        hospital.setEmail(dto.getEmail());
        hospital.setAddress(dto.getAddress());
        hospital.setPhone(dto.getPhone());
        hospital.setHospitalPackage(dto.getPackageId());
        hospital.setPLimit(dto.getP_limit());
        hospital.setDLimit(dto.getD_limit());
        if (dto.getModule() != null) {
            hospital.setModule(String.join(",", dto.getModule()));
        }

        if (StringUtils.hasText(hospital.getIonUserId())) {
            User user = userRepository.findById(Long.valueOf(hospital.getIonUserId()))
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setUsername(dto.getName());
            user.setEmail(dto.getEmail());
            user.setPhone(dto.getPhone());
            if (StringUtils.hasText(dto.getPassword())) {
                user.setPassword(passwordEncoder.encode(dto.getPassword().trim()));
            }
            userRepository.save(user);
        }

        hospitalRepository.save(hospital);

        if (StringUtils.hasText(hospital.getIonUserId())) {
            Settings settings = settingsRepository.findByHospitalId(hospital.getIonUserId()).orElse(new Settings());
            settings.setHospitalId(hospital.getIonUserId());
            settings.setTitle(dto.getName());
            settings.setEmail(dto.getEmail());
            settings.setAddress(dto.getAddress());
            settings.setPhone(dto.getPhone());
            if (StringUtils.hasText(dto.getLanguage())) {
                settings.setLanguage(dto.getLanguage().trim());
            }
            settingsRepository.save(settings);
        }

        return hospital;
    }

    @Transactional
    public HospitalResponseDto updateHospitalUsage(Integer id, String usageStatus) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hospital not found"));

        Settings settings = findOrCreateHospitalSettings(hospital);
        settings.setCodecUsername(normalizeUsageStatus(usageStatus));
        settingsRepository.save(settings);

        return toResponseDto(hospital);
    }

    @Transactional(readOnly = true)
    public boolean isHospitalUsageStopped(String hospitalScopeId) {
        for (String scopeId : resolveHospitalSettingsScopeIds(hospitalScopeId)) {
            if (!StringUtils.hasText(scopeId)) {
                continue;
            }

            String usageStatus = settingsRepository.findByHospitalId(scopeId)
                    .map(Settings::getCodecUsername)
                    .filter(StringUtils::hasText)
                    .map(this::normalizeUsageStatus)
                    .orElse(null);

            if (USAGE_STATUS_STOPPED.equalsIgnoreCase(usageStatus)) {
                return true;
            }

            if (StringUtils.hasText(usageStatus)) {
                return false;
            }
        }

        return false;
    }

    public void deleteHospital(Integer id) {
        Hospital hospital = hospitalRepository.findById(id).orElse(null);
        if (hospital != null) {
            if (StringUtils.hasText(hospital.getIonUserId())) {
                userRepository.deleteById(Long.valueOf(hospital.getIonUserId()));
            }
            hospitalRepository.deleteById(id);
        }
    }

    private void validateCreateRequest(HospitalRequestDto dto) {
        if (!StringUtils.hasText(dto.getName())) {
            throw new IllegalArgumentException("Hospital name is required.");
        }
        if (!StringUtils.hasText(dto.getEmail())) {
            throw new IllegalArgumentException("Hospital email is required.");
        }
        if (!StringUtils.hasText(dto.getPhone())) {
            throw new IllegalArgumentException("Hospital phone is required.");
        }
        if (!StringUtils.hasText(dto.getAddress())) {
            throw new IllegalArgumentException("Hospital address is required.");
        }
    }

    private Group resolveAdminGroup() {
        return groupRepository.findByName("admin")
                .orElseGet(() -> groupRepository.findAll().stream()
                        .filter(group -> "admin".equalsIgnoreCase(group.getName()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Admin group is not configured.")));
    }

    private String resolveAdminPassword(HospitalRequestDto dto) {
        return StringUtils.hasText(dto.getPassword())
                ? dto.getPassword().trim()
                : DEFAULT_BRANCH_ADMIN_PASSWORD;
    }

    private void seedHospitalDefaults(HospitalRequestDto dto, String tenantHospitalId, Long adminUserId) {
        Settings settings = new Settings();
        settings.setHospitalId(tenantHospitalId);
        settings.setTitle(dto.getName().trim());
        settings.setEmail(dto.getEmail().trim().toLowerCase());
        settings.setAddress(trim(dto.getAddress()));
        settings.setPhone(trim(dto.getPhone()));
        settings.setLanguage(StringUtils.hasText(dto.getLanguage()) ? dto.getLanguage().trim() : "english");
        settings.setSystemVendor("Code Aristos | Hospital management System");
        settings.setDiscount("flat");
        settings.setCurrency("$");
        settings.setCodecUsername(USAGE_STATUS_ACTIVE);
        settingsRepository.save(settings);

        String[] bloodGroups = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        for (String group : bloodGroups) {
            BloodBank bank = new BloodBank();
            bank.setGroup(group);
            bank.setStatus("0 Bags");
            bank.setHospitalId(tenantHospitalId);
            bloodBankRepository.save(bank);
        }

        SmsSettings clickatell = new SmsSettings();
        clickatell.setName("Clickatell");
        clickatell.setUsername("Your ClickAtell Username");
        clickatell.setPassword("Your ClickAtell Password");
        clickatell.setApiId("Your ClickAtell Api Id");
        clickatell.setUser(String.valueOf(adminUserId));
        clickatell.setHospitalId(tenantHospitalId);
        smsSettingsRepository.save(clickatell);

        SmsSettings msg91 = new SmsSettings();
        msg91.setName("MSG91");
        msg91.setUsername("Your MSG91 Username");
        msg91.setApiId("Your MSG91 API ID");
        msg91.setAuthkey("Your MSG91 Auth Key");
        msg91.setUser(String.valueOf(adminUserId));
        msg91.setHospitalId(tenantHospitalId);
        smsSettingsRepository.save(msg91);

        PaymentGateway paypal = new PaymentGateway();
        paypal.setName("PayPal");
        paypal.setApiUsername("PayPal API Username");
        paypal.setApiPassword("PayPal API Password");
        paypal.setApiSignature("PayPal API Signature");
        paypal.setStatus("test");
        paypal.setHospitalId(tenantHospitalId);
        paymentGatewayRepository.save(paypal);

        PaymentGateway payu = new PaymentGateway();
        payu.setName("Pay U Money");
        payu.setMerchantKey("Merchant key");
        payu.setSalt("Salt");
        payu.setStatus("test");
        payu.setHospitalId(tenantHospitalId);
        paymentGatewayRepository.save(payu);

        EmailSettings emailSettings = new EmailSettings();
        emailSettings.setAdminEmail(dto.getEmail().trim().toLowerCase());
        emailSettings.setHospitalId(tenantHospitalId);
        emailSettingsRepository.save(emailSettings);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String getHospitalUsageStatus(Hospital hospital) {
        for (String scopeId : resolveHospitalSettingsScopeIds(hospital)) {
            if (!StringUtils.hasText(scopeId)) {
                continue;
            }

            String usageStatus = settingsRepository.findByHospitalId(scopeId)
                    .map(Settings::getCodecUsername)
                    .filter(StringUtils::hasText)
                    .map(this::normalizeUsageStatus)
                    .orElse(null);

            if (StringUtils.hasText(usageStatus)) {
                return usageStatus;
            }
        }

        return USAGE_STATUS_ACTIVE;
    }

    private Settings findOrCreateHospitalSettings(Hospital hospital) {
        for (String scopeId : resolveHospitalSettingsScopeIds(hospital)) {
            if (!StringUtils.hasText(scopeId)) {
                continue;
            }

            Settings existing = settingsRepository.findByHospitalId(scopeId).orElse(null);
            if (existing != null) {
                return existing;
            }
        }

        Settings settings = new Settings();
        String preferredScopeId = StringUtils.hasText(hospital.getIonUserId())
                ? hospital.getIonUserId().trim()
                : String.valueOf(hospital.getId());
        settings.setHospitalId(preferredScopeId);
        settings.setTitle(hospital.getName());
        settings.setEmail(hospital.getEmail());
        settings.setAddress(hospital.getAddress());
        settings.setPhone(hospital.getPhone());
        settings.setLanguage("english");
        settings.setDiscount("flat");
        settings.setCurrency("$");
        settings.setCodecUsername(USAGE_STATUS_ACTIVE);
        return settings;
    }

    private List<String> resolveHospitalSettingsScopeIds(Hospital hospital) {
        List<String> scopeIds = new java.util.ArrayList<>();
        if (hospital == null) {
            return scopeIds;
        }

        if (StringUtils.hasText(hospital.getIonUserId())) {
            scopeIds.add(hospital.getIonUserId().trim());
        }
        scopeIds.add(String.valueOf(hospital.getId()));
        return scopeIds;
    }

    private List<String> resolveHospitalSettingsScopeIds(String hospitalScopeId) {
        if (!StringUtils.hasText(hospitalScopeId)) {
            return java.util.List.of();
        }

        String trimmed = hospitalScopeId.trim();
        Hospital hospital = hospitalRepository.findByIonUserId(trimmed)
                .orElseGet(() -> {
                    try {
                        return hospitalRepository.findById(Integer.parseInt(trimmed)).orElse(null);
                    } catch (NumberFormatException ignored) {
                        return null;
                    }
                });

        if (hospital != null) {
            return resolveHospitalSettingsScopeIds(hospital);
        }

        return java.util.List.of(trimmed);
    }

    private String normalizeUsageStatus(String usageStatus) {
        if (!StringUtils.hasText(usageStatus)) {
            return USAGE_STATUS_ACTIVE;
        }

        return "stopped".equalsIgnoreCase(usageStatus.trim())
                ? USAGE_STATUS_STOPPED
                : USAGE_STATUS_ACTIVE;
    }
}
