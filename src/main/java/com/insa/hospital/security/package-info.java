/**
 * Security package.
 *
 * Contains Spring Security configuration and JWT utilities.
 * Files to be created in the Authentication module (Part 3):
 *  - SecurityConfig.java       — Filter chain, role-based access rules
 *  - JwtTokenProvider.java     — Token generation & validation
 *  - JwtAuthenticationFilter.java — OncePerRequestFilter for JWT extraction
 *  - JwtAuthEntryPoint.java    — 401 handler for unauthenticated requests
 *  - UserDetailsServiceImpl.java — Loads user from legacy `users` table
 */
package com.insa.hospital.security;
