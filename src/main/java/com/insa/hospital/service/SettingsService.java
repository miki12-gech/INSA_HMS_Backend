package com.insa.hospital.service;

import com.insa.hospital.entity.Hospital;
import com.insa.hospital.entity.Settings;
import com.insa.hospital.repository.HospitalRepository;
import com.insa.hospital.repository.SettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SettingsService {
    public static final String GLOBAL_SCOPE_ID = "ALL";

    private final SettingsRepository settingsRepository;
    private final HospitalRepository hospitalRepository;
    private final TenantAccessService tenantAccessService;

    public Settings getSettings(String hospitalId) {
        return resolveSettings(hospitalId);
    }

    public Settings updateSettings(String hospitalId, Settings newSettings) {
        String normalizedHospitalId = normalizeSettingsScopeId(hospitalId);
        Settings settings = findExistingSettings(normalizedHospitalId)
                .orElseGet(() -> buildDefaultSettings(normalizedHospitalId));

        settings.setHospitalId(normalizedHospitalId);
        if (isGlobalScope(normalizedHospitalId)) {
            applyGlobalContentSettings(settings, newSettings);
            return settingsRepository.save(settings);
        }

        settings.setSystemVendor(newSettings.getSystemVendor());
        settings.setTitle(newSettings.getTitle());
        settings.setAddress(newSettings.getAddress());
        settings.setPhone(newSettings.getPhone());
        settings.setEmail(newSettings.getEmail());
        settings.setCurrency(newSettings.getCurrency());
        settings.setLanguage(newSettings.getLanguage());
        settings.setDiscount(newSettings.getDiscount());
        settings.setVat(newSettings.getVat());
        settings.setLoginTitle(newSettings.getLoginTitle());
        settings.setHomepageTitle(newSettings.getHomepageTitle());
        settings.setHomepageDescription(newSettings.getHomepageDescription());
        settings.setFooterText(newSettings.getFooterText());
        settings.setFooterTagline(newSettings.getFooterTagline());
        settings.setPrimaryColor(newSettings.getPrimaryColor());
        settings.setPrimaryDarkColor(newSettings.getPrimaryDarkColor());
        settings.setAccentColor(newSettings.getAccentColor());
        settings.setBackgroundColor(newSettings.getBackgroundColor());
        settings.setSurfaceColor(newSettings.getSurfaceColor());
        settings.setTextColor(newSettings.getTextColor());
        settings.setPaymentGateway(newSettings.getPaymentGateway());
        settings.setSmsGateway(newSettings.getSmsGateway());
        if (newSettings.getCodecUsername() != null) {
            settings.setCodecUsername(newSettings.getCodecUsername());
        }
        if (newSettings.getCodecPurchaseCode() != null) {
            settings.setCodecPurchaseCode(newSettings.getCodecPurchaseCode());
        }
        if (StringUtils.hasText(newSettings.getLogo())) {
            settings.setLogo(newSettings.getLogo());
        }
        if (StringUtils.hasText(newSettings.getInvoiceLogo())) {
            settings.setInvoiceLogo(newSettings.getInvoiceLogo());
        }
        return settingsRepository.save(settings);
    }

    public String uploadLogo(MultipartFile file) throws IOException {
        Path uploadsDir = Paths.get("uploads").toAbsolutePath().normalize();
        Files.createDirectories(uploadsDir);

        String originalFilename = Optional.ofNullable(file.getOriginalFilename()).orElse("logo");
        String cleanedFilename = org.springframework.util.StringUtils.cleanPath(originalFilename);
        int extensionIndex = cleanedFilename.lastIndexOf('.');
        String extension = extensionIndex >= 0 ? cleanedFilename.substring(extensionIndex) : "";
        String fileName = UUID.randomUUID() + extension;

        Path targetPath = uploadsDir.resolve(fileName).normalize();
        file.transferTo(targetPath);
        return "uploads/" + fileName;
    }

    public void validateHospitalManagementAccess(String hospitalId) {
        if (isGlobalScope(hospitalId)) {
            if (!tenantAccessService.isSuperAdmin()) {
                throw new AccessDeniedException("Only superadmin can manage global login, dashboard home, and shared palette settings.");
            }
            return;
        }

        if (tenantAccessService.isSuperAdmin()) {
            return;
        }

        boolean allowed = tenantAccessService.resolveHospitalScopeIds(hospitalId).stream()
                .anyMatch(tenantAccessService::belongsToCurrentScope);

        if (!allowed) {
            throw new AccessDeniedException("You do not have permission to manage this hospital settings profile.");
        }
    }

    private Settings resolveSettings(String hospitalId) {
        String normalizedHospitalId = normalizeSettingsScopeId(hospitalId);

        if (isGlobalScope(normalizedHospitalId)) {
            return findExistingSettings(normalizedHospitalId)
                    .orElseGet(() -> buildDefaultSettings(normalizedHospitalId));
        }

        Settings resolved = findExistingSettings(normalizedHospitalId)
                .orElseGet(() -> buildDefaultSettings(normalizedHospitalId));

        findExistingSettings(GLOBAL_SCOPE_ID).ifPresent(globalSettings ->
                applyGlobalContentOverrides(resolved, globalSettings));

        return resolved;
    }

    private Optional<Settings> findExistingSettings(String hospitalId) {
        if (isGlobalScope(hospitalId)) {
            return settingsRepository.findByHospitalId(GLOBAL_SCOPE_ID);
        }

        for (String scopeId : tenantAccessService.resolveHospitalScopeIds(hospitalId)) {
            Optional<Settings> settings = settingsRepository.findByHospitalId(scopeId);
            if (settings.isPresent()) {
                return settings;
            }
        }

        return Optional.empty();
    }

    private Settings buildDefaultSettings(String hospitalId) {
        Settings settings = new Settings();
        settings.setHospitalId(hospitalId);
        settings.setSystemVendor("Hospital Management System");
        settings.setLoginTitle("Secure staff access");
        settings.setHomepageTitle("Trusted healthcare for every visit");
        settings.setHomepageDescription(
                "Manage referrals, appointments, records, and daily hospital operations from one secure workspace."
        );
        settings.setFooterText("Delivering coordinated care through a single hospital operations platform.");
        settings.setFooterTagline("Hospital branding and patient services");
        settings.setPrimaryColor("#2B3B8A");
        settings.setPrimaryDarkColor("#0F1740");
        settings.setAccentColor("#A92338");
        settings.setBackgroundColor("#F8FAFC");
        settings.setSurfaceColor("#FFFFFF");
        settings.setTextColor("#0F172A");

        if (isGlobalScope(hospitalId)) {
            settings.setTitle("All Hospitals");
            return settings;
        }

        findHospital(hospitalId).ifPresent(hospital -> {
            settings.setTitle(hospital.getName());
            settings.setAddress(hospital.getAddress());
            settings.setPhone(hospital.getPhone());
            settings.setEmail(hospital.getEmail());
            settings.setHomepageTitle(hospital.getName() + " Care Portal");
            settings.setFooterTagline(hospital.getName());
        });

        return settings;
    }

    private String normalizeSettingsScopeId(String hospitalId) {
        if (isGlobalScope(hospitalId)) {
            return GLOBAL_SCOPE_ID;
        }
        return tenantAccessService.normalizeHospitalScopeId(hospitalId);
    }

    private boolean isGlobalScope(String hospitalId) {
        return StringUtils.hasText(hospitalId)
                && GLOBAL_SCOPE_ID.equalsIgnoreCase(hospitalId.trim());
    }

    private void applyGlobalContentSettings(Settings target, Settings source) {
        target.setLoginTitle(source.getLoginTitle());
        target.setHomepageTitle(source.getHomepageTitle());
        target.setHomepageDescription(source.getHomepageDescription());
        target.setPrimaryColor(source.getPrimaryColor());
        target.setPrimaryDarkColor(source.getPrimaryDarkColor());
        target.setAccentColor(source.getAccentColor());
        target.setBackgroundColor(source.getBackgroundColor());
        target.setSurfaceColor(source.getSurfaceColor());
        target.setTextColor(source.getTextColor());
    }

    private void applyGlobalContentOverrides(Settings branchSettings, Settings globalSettings) {
        if (StringUtils.hasText(globalSettings.getLoginTitle())) {
            branchSettings.setLoginTitle(globalSettings.getLoginTitle());
        }
        if (StringUtils.hasText(globalSettings.getHomepageTitle())) {
            branchSettings.setHomepageTitle(globalSettings.getHomepageTitle());
        }
        if (StringUtils.hasText(globalSettings.getHomepageDescription())) {
            branchSettings.setHomepageDescription(globalSettings.getHomepageDescription());
        }
        if (StringUtils.hasText(globalSettings.getPrimaryColor())) {
            branchSettings.setPrimaryColor(globalSettings.getPrimaryColor());
        }
        if (StringUtils.hasText(globalSettings.getPrimaryDarkColor())) {
            branchSettings.setPrimaryDarkColor(globalSettings.getPrimaryDarkColor());
        }
        if (StringUtils.hasText(globalSettings.getAccentColor())) {
            branchSettings.setAccentColor(globalSettings.getAccentColor());
        }
        if (StringUtils.hasText(globalSettings.getBackgroundColor())) {
            branchSettings.setBackgroundColor(globalSettings.getBackgroundColor());
        }
        if (StringUtils.hasText(globalSettings.getSurfaceColor())) {
            branchSettings.setSurfaceColor(globalSettings.getSurfaceColor());
        }
        if (StringUtils.hasText(globalSettings.getTextColor())) {
            branchSettings.setTextColor(globalSettings.getTextColor());
        }
    }

    private Optional<Hospital> findHospital(String hospitalId) {
        if (hospitalRepository.findByIonUserId(hospitalId).isPresent()) {
            return hospitalRepository.findByIonUserId(hospitalId);
        }

        try {
            return hospitalRepository.findById(Integer.parseInt(hospitalId));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }
}

