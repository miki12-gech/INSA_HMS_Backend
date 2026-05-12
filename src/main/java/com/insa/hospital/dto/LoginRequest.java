package com.insa.hospital.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for POST /api/auth/login.
 * The client sends email + password as JSON.
 */
public record LoginRequest(

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    String email,

    @NotBlank(message = "Password is required")
    String password

) {}
