package com.insa.hospital.util;

import com.insa.hospital.exception.BadRequestException;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class StaffEmailPolicy {

    public static final String REQUIRED_DOMAIN = "@insa.com";

    private static final Pattern STAFF_EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@insa\\.com$", Pattern.CASE_INSENSITIVE);

    private static final Set<String> STAFF_ROLES = Set.of(
            "doctor",
            "nurse",
            "pharmacist",
            "laboratorist",
            "receptionist",
            "accountant",
            "economist",
            "admin"
    );

    private StaffEmailPolicy() {
    }

    public static String normalizeStaffEmail(String rawEmail) {
        if (!StringUtils.hasText(rawEmail)) {
            throw new BadRequestException("Staff email is required.");
        }

        String normalizedEmail = rawEmail.trim().toLowerCase(Locale.ROOT);
        if (!STAFF_EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new BadRequestException("Staff email must end with @insa.com.");
        }

        return normalizedEmail;
    }

    public static boolean requiresInsaStaffEmail(String roleName) {
        if (!StringUtils.hasText(roleName)) {
            return false;
        }

        return STAFF_ROLES.contains(roleName.trim().toLowerCase(Locale.ROOT));
    }
}
