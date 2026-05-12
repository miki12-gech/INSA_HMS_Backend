package com.insa.hospital.config;

import com.insa.hospital.security.CustomUserDetailsService;
import com.insa.hospital.security.JwtAuthEntryPoint;
import com.insa.hospital.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Production Spring Security Configuration (Spring Security 6)
 *
 * Design:
 * - Stateless JWT — no HttpSession is created.
 * - BCryptPasswordEncoder is compatible with PHP's $2y$ hash prefix.
 * - Public endpoints: /api/health, /api/auth/**
 * - Self-service endpoints under /api/self-service/** are restricted to EMPLOYEE users.
 * - IAM endpoints under /api/admin/** and department mutations are restricted
 *   to the SUPERADMIN role at the filter-chain level.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthEntryPoint jwtAuthEntryPoint;

    @Autowired
    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          JwtAuthenticationFilter jwtAuthenticationFilter,
                          JwtAuthEntryPoint jwtAuthEntryPoint) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthEntryPoint = jwtAuthEntryPoint;
    }

    /** Public endpoints that do not require authentication. */
    private static final String[] PUBLIC_URLS = {
            "/api/health",
            "/api/auth/**",
            "/api/settings/public",
            "/api/labs/machine/receive",
            "/uploads/**",
            "/error"
    };

    /** Explicitly listing module endpoints for frontend dev phase to ensure .authenticated() */
    private static final String[] MODULE_URLS = {
            "/api/pharmacy/**",
            "/api/triage/**",
            "/api/prescriptions/**",
            "/api/patients/**",
            "/api/doctors/**",
            "/api/nurses/**",
            "/api/referrals/**"
    };

    private static final String[] SELF_SERVICE_URLS = {
            "/api/self-service/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   PasswordEncoder passwordEncoder) throws Exception {
        http
            // Disable CSRF — stateless JWT API does not need it
            .csrf(AbstractHttpConfigurer::disable)

            // Configure 401 response for missing/invalid tokens
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(jwtAuthEntryPoint))

            // Stateless sessions — no HttpSession is ever created
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Access control rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(PUBLIC_URLS).permitAll()
                // Superadmin IAM management
                .requestMatchers("/api/admin/**").hasAuthority("ROLE_superadmin")
                .requestMatchers(HttpMethod.POST, "/api/hospitals/add").hasAuthority("ROLE_superadmin")
                .requestMatchers(HttpMethod.POST, "/hospital/addNew").hasAuthority("ROLE_superadmin")
                .requestMatchers(HttpMethod.POST, "/api/departments/add").hasAuthority("ROLE_superadmin")
                .requestMatchers(HttpMethod.PUT, "/api/departments/**").hasAuthority("ROLE_superadmin")
                .requestMatchers(HttpMethod.DELETE, "/api/departments/**").hasAuthority("ROLE_superadmin")
                // Employee self-service workflow
                .requestMatchers(SELF_SERVICE_URLS).hasAnyRole("EMPLOYEE", "Employee")
                // Target modules restricted to JWT holders without strict role enforcement
                .requestMatchers(MODULE_URLS).authenticated()
                // Everything else requires authentication
                .anyRequest().authenticated())

            // Wire the custom authentication provider (UserDetailsService + BCrypt)
            .authenticationProvider(authenticationProvider(passwordEncoder))

            // Register JWT filter BEFORE UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * BCryptPasswordEncoder — compatible with PHP password_hash() ($2y$) hashes.
     */
    /**
     * Authentication provider that ties together the UserDetailsService
     * and the PasswordEncoder (BCrypt).
     */
    @Bean
    public AuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    /**
     * Exposes the AuthenticationManager as a Spring Bean so it can be
     * injected into AuthController to trigger authentication programmatically.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
