package com.insa.hospital.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT Service — token generation, validation, and claims extraction.
 *
 * Uses JJWT 0.12.x API (io.jsonwebtoken).
 * Secret key is read from application.yml → app.jwt.secret (HEX string).
 * Token expiry is read from app.jwt.expiration-ms (default 24h).
 *
 * Claims embedded in every token:
 *   - sub  : user email (the principal identifier)
 *   - role : raw group name (e.g. "Doctor", "admin", "superadmin")
 *   - hospitalId : the user's hospital_ion_id from the users table
 *   - iat  : issued-at timestamp
 *   - exp  : expiry timestamp
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    // ─── Token Generation ─────────────────────────────────────────────────────

    /**
     * Generates a JWT token from a Spring Security Authentication object.
     * Extracts role and hospital info from custom extra claims map.
     */
    public String generateToken(Authentication authentication, Map<String, Object> extraClaims) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return buildToken(extraClaims, userDetails.getUsername(), jwtExpirationMs);
    }

    /**
     * Generates a JWT token directly from email + extra claims.
     * Used by AuthController after successful authentication.
     */
    public String generateToken(String email, Map<String, Object> extraClaims) {
        return buildToken(extraClaims, email, jwtExpirationMs);
    }

    private String buildToken(Map<String, Object> extraClaims, String subject, long expirationMs) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    // ─── Claims Extraction ────────────────────────────────────────────────────

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public String extractHospitalId(String token) {
        return extractClaim(token, claims -> claims.get("hospitalId", String.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ─── Validation ───────────────────────────────────────────────────────────

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String email = extractEmail(token);
            return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.warn("Invalid JWT signature or token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
        }
        return false;
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // ─── Key Helper ───────────────────────────────────────────────────────────

   private SecretKey getSigningKey() {
        // The secret in application.yml is a hex string — decode it directly.
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret); // <--- አስተካክዬዋለሁ!
        return Keys.hmacShaKeyFor(keyBytes);
    }
}