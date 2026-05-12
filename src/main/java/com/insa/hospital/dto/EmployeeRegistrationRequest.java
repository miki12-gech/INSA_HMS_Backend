package com.insa.hospital.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Public self-registration request for new employee portal accounts.
 */
public record EmployeeRegistrationRequest(

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must be at most 100 characters")
    String name,

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    @Size(max = 100, message = "Email must be at most 100 characters")
    String email,

    @NotBlank(message = "Phone number is required")
    @Size(max = 20, message = "Phone number must be at most 20 characters")
    String phone,

    @NotBlank(message = "INSA ID card number is required")
    @Size(max = 100, message = "INSA ID card number must be at most 100 characters")
    String insaIdCardNumber,

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 255, message = "Password must be between 8 and 255 characters")
    String password

) {}
