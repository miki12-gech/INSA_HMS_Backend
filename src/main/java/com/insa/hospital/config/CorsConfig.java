package com.insa.hospital.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * Global CORS Configuration
 *
 * Allows the Next.js frontend (http://localhost:3000) and any other
 * local dev origins to communicate with this API without CORS errors.
 *
 * Permitted methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
 * Permitted headers: all
 * Credentials: allowed (required for JWT cookie support if needed)
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();

        // ─── Allowed Origins ──────────────────────────────────────────────
        // Add the Next.js dev server and any production domain here.
        corsConfiguration.setAllowedOrigins(List.of(
                "http://localhost:3000",   // Next.js dev server
                "http://localhost:3001",   // alternate Next.js port
                "http://127.0.0.1:3000"   // localhost alias
        ));

        // ─── Allowed HTTP Methods ─────────────────────────────────────────
        corsConfiguration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // ─── Allowed Headers ──────────────────────────────────────────────
        // Allow all headers so that Authorization, Content-Type, etc. pass.
        corsConfiguration.setAllowedHeaders(List.of("*"));

        // ─── Expose Headers ───────────────────────────────────────────────
        // Allow the frontend to read the Authorization header from responses.
        corsConfiguration.setExposedHeaders(List.of("Authorization", "X-Total-Count"));

        // ─── Credentials ──────────────────────────────────────────────────
        // Required if sending cookies or Authorization header cross-origin.
        corsConfiguration.setAllowCredentials(true);

        // ─── Pre-flight Cache ─────────────────────────────────────────────
        // Cache pre-flight responses for 1 hour (3600 seconds).
        corsConfiguration.setMaxAge(3600L);

        // Apply this configuration to all API endpoints.
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);

        return new CorsFilter(source);
    }
}
