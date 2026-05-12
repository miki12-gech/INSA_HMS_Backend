package com.insa.hospital.dto;

/**
 * Response DTO returned by POST /api/auth/login on success.
 *
 * Contains the JWT token and just enough user context for the frontend
 * to render the correct dashboard without an additional API call.
 *
 * Fields:
 *  - token      : The JWT Bearer token (store in memory/cookie on frontend)
 *  - tokenType  : Always "Bearer"
 *  - id         : Legacy users.id
 *  - email      : users.email
 *  - username   : users.username (display name)
 *  - role       : Raw group name from legacy groups.name (e.g. "Doctor", "admin")
 *  - hospitalId : users.hospital_ion_id (multi-tenant identifier)
 */
public record LoginResponse(
    String token,
    String tokenType,
    Long id,
    String email,
    String username,
    String role,
    String hospitalId,
    String hospitalName
) {
    /** Convenience factory used by AuthController. */
    public static LoginResponse of(String token, Long id, String email,
                                    String username, String role, String hospitalId,
                                    String hospitalName) {
        return new LoginResponse(token, "Bearer", id, email, username, role, hospitalId, hospitalName);
    }
}
