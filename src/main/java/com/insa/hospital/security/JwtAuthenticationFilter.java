package com.insa.hospital.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import com.insa.hospital.service.HospitalService;

import java.io.IOException;

/**
 * JWT Authentication Filter — runs once per HTTP request.
 *
 * Intercepts every request and:
 *  1. Extracts the Bearer token from the Authorization header.
 *  2. Validates the token signature and expiry using JwtService.
 *  3. Loads the user from the DB via CustomUserDetailsService.
 *  4. Sets the authentication in the SecurityContext so downstream
 *     controllers know who made the request and what role they have.
 *
 * If no valid token is present, the request proceeds unauthenticated
 * and Spring Security's access rules determine if it is allowed.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final HospitalService hospitalService;

    @Autowired
    public JwtAuthenticationFilter(JwtService jwtService,
                                    CustomUserDetailsService userDetailsService,
                                    HospitalService hospitalService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.hospitalService = hospitalService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // 1. Extract token from Authorization header
        String jwt = extractJwtFromRequest(request);

        try {
            if (StringUtils.hasText(jwt) && jwtService.validateToken(jwt)) {
                try {
                    // 2. Extract email from token claims
                    String email = jwtService.extractEmail(jwt);

                    // 3. Populate JwtContextHolder with hospital/user claims
                    //    (available to all downstream controllers and services)
                    String hospitalId = jwtService.extractHospitalId(jwt);
                    String userId     = jwtService.extractClaim(jwt,
                            claims -> claims.get("userId", String.class));
                    String role       = jwtService.extractRole(jwt);
                    JwtContextHolder.setHospitalId(hospitalId);
                    JwtContextHolder.setUserId(userId);
                    JwtContextHolder.setRole(role);

                    if (!"superadmin".equalsIgnoreCase(role)
                            && hospitalService.isHospitalUsageStopped(hospitalId)) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.getWriter().write(
                                "{\"message\":\"Hospital access has been stopped by superadmin\"}");
                        return;
                    }

                    // 4. Only authenticate if not already authenticated
                    if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                        // 5. Load UserDetails from the legacy users table
                        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                        // 6. Validate token against the loaded user
                        if (jwtService.isTokenValid(jwt, userDetails)) {

                            // 7. Set authentication in SecurityContext
                            UsernamePasswordAuthenticationToken authToken =
                                    new UsernamePasswordAuthenticationToken(
                                            userDetails,
                                            null,
                                            userDetails.getAuthorities()
                                    );
                            authToken.setDetails(
                                    new WebAuthenticationDetailsSource().buildDetails(request));

                            SecurityContextHolder.getContext().setAuthentication(authToken);
                            log.debug("Authenticated user '{}' with role(s): {}",
                                    email, userDetails.getAuthorities());
                        }
                    }
                } catch (Exception e) {
                    log.error("Could not set user authentication in security context: {}", e.getMessage());
                }
            }

            filterChain.doFilter(request, response);

        } finally {
            // Always clear thread-local claims to prevent leaks across requests
            JwtContextHolder.clear();
        }
    }

    /**
     * Extracts the raw JWT string from the "Authorization: Bearer <token>" header.
     * Returns null if the header is absent or malformed.
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
