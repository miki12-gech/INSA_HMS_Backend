package com.insa.hospital.security;

/**
 * Thread-local holder for JWT claims extracted by {@link JwtAuthenticationFilter}.
 *
 * Avoids re-parsing the JWT token in every controller/service.
 * The filter populates these values once per request; the controller reads them.
 *
 * Pattern: Set in JwtAuthenticationFilter → Read in controllers.
 * Cleared automatically in JwtAuthenticationFilter's finally block to prevent leaks.
 */
public final class JwtContextHolder {

    private static final ThreadLocal<String> hospitalIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> userIdHolder     = new ThreadLocal<>();
    private static final ThreadLocal<String> roleHolder       = new ThreadLocal<>();

    private JwtContextHolder() {}

    // ─── Set (called by JwtAuthenticationFilter) ─────────────────────────────

    public static void setHospitalId(String hospitalId) {
        hospitalIdHolder.set(hospitalId);
    }

    public static void setUserId(String userId) {
        userIdHolder.set(userId);
    }

    public static void setRole(String role) {
        roleHolder.set(role);
    }

    // ─── Get (called by controllers and services) ─────────────────────────────

    public static String getHospitalId() {
        return hospitalIdHolder.get();
    }

    public static String getUserId() {
        return userIdHolder.get();
    }

    public static String getRole() {
        return roleHolder.get();
    }

    // ─── Clear (called by JwtAuthenticationFilter in finally block) ───────────

    public static void clear() {
        hospitalIdHolder.remove();
        userIdHolder.remove();
        roleHolder.remove();
    }
}
