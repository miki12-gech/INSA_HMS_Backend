package com.insa.hospital.service;

import com.insa.hospital.entity.Hospital;
import com.insa.hospital.repository.HospitalRepository;
import com.insa.hospital.security.JwtContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TenantAccessService {

    private static final String SUPERADMIN_ROLE = "superadmin";
    private static final String DEFAULT_MAIN_HOSPITAL_NAME = "INSA MEDICAL CENTER";

    private final HospitalRepository hospitalRepository;

    public String getCurrentRole() {
        if (StringUtils.hasText(JwtContextHolder.getRole())) {
            return JwtContextHolder.getRole().trim();
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return "";
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(StringUtils::hasText)
                .map(authority -> authority.startsWith("ROLE_") ? authority.substring(5) : authority)
                .findFirst()
                .orElse("");
    }

    public boolean isSuperAdmin() {
        return SUPERADMIN_ROLE.equalsIgnoreCase(getCurrentRole());
    }

    public String getCurrentHospitalId() {
        return normalizeHospitalScopeId(JwtContextHolder.getHospitalId());
    }

    public List<String> getCurrentHospitalScopeIds() {
        return resolveHospitalScopeIds(getCurrentHospitalId());
    }

    public List<String> getAllHospitalScopeIds() {
        LinkedHashSet<String> scopeIds = new LinkedHashSet<>();

        for (Hospital hospital : hospitalRepository.findAll()) {
            if (StringUtils.hasText(hospital.getIonUserId())) {
                scopeIds.add(hospital.getIonUserId().trim());
            }
            scopeIds.add(String.valueOf(hospital.getId()));
        }

        String defaultHospitalId = getDefaultMainHospitalId();
        if (StringUtils.hasText(defaultHospitalId)) {
            scopeIds.add(defaultHospitalId);
        }

        return new ArrayList<>(scopeIds);
    }

    public boolean includeBlankHospitalRowsForCurrentUser() {
        String defaultHospitalId = getDefaultMainHospitalId();
        return StringUtils.hasText(defaultHospitalId)
                && getCurrentHospitalScopeIds().contains(defaultHospitalId);
    }

    public boolean belongsToCurrentScope(String recordHospitalId) {
        if (isSuperAdmin()) {
            return true;
        }

        String effectiveHospitalId = normalizeRecordHospitalId(recordHospitalId);
        return getCurrentHospitalScopeIds().contains(effectiveHospitalId);
    }

    public String normalizeRecordHospitalId(String recordHospitalId) {
        if (!StringUtils.hasText(recordHospitalId)) {
            return getDefaultMainHospitalId();
        }
        return normalizeHospitalScopeId(recordHospitalId);
    }

    public String getDefaultMainHospitalId() {
        Optional<Hospital> defaultHospital = hospitalRepository.findByNameIgnoreCase(DEFAULT_MAIN_HOSPITAL_NAME);
        if (defaultHospital.isPresent()) {
            Hospital hospital = defaultHospital.get();
            if (StringUtils.hasText(hospital.getIonUserId())) {
                return hospital.getIonUserId().trim();
            }
            return String.valueOf(hospital.getId());
        }

        return hospitalRepository.findFirstByOrderByIdAsc()
                .map(hospital -> StringUtils.hasText(hospital.getIonUserId())
                        ? hospital.getIonUserId().trim()
                        : String.valueOf(hospital.getId()))
                .orElse("");
    }

    public String normalizeHospitalScopeId(String hospitalId) {
        String fallbackHospitalId = getDefaultMainHospitalId();
        if (!StringUtils.hasText(hospitalId)) {
            return fallbackHospitalId;
        }

        String trimmed = hospitalId.trim();
        if (hospitalRepository.findByIonUserId(trimmed).isPresent()) {
            return trimmed;
        }

        try {
            return hospitalRepository.findById(Integer.parseInt(trimmed))
                    .map(Hospital::getIonUserId)
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .orElse(trimmed);
        } catch (NumberFormatException ignored) {
            return trimmed;
        }
    }

    public List<String> resolveHospitalScopeIds(String hospitalId) {
        LinkedHashSet<String> scopeIds = new LinkedHashSet<>();
        String normalizedHospitalId = normalizeHospitalScopeId(hospitalId);

        if (StringUtils.hasText(normalizedHospitalId)) {
            scopeIds.add(normalizedHospitalId);
            hospitalRepository.findByIonUserId(normalizedHospitalId)
                    .map(Hospital::getId)
                    .map(String::valueOf)
                    .ifPresent(scopeIds::add);
        }

        if (StringUtils.hasText(hospitalId)) {
            scopeIds.add(hospitalId.trim());
        }

        return new ArrayList<>(scopeIds);
    }

    public boolean isSuperAdminRole(String role) {
        return StringUtils.hasText(role)
                && SUPERADMIN_ROLE.equalsIgnoreCase(role.trim().toLowerCase(Locale.ROOT));
    }
}
