package com.insa.hospital.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AllianceHospitalRequestDto(

        @NotBlank(message = "Hospital name is required")
        @Size(max = 255)
        String name,

        @NotBlank(message = "Address is required")
        @Size(max = 255)
        String address,

        @NotBlank(message = "Phone number is required")
        @Size(max = 50)
        String phone,

        @Email(message = "Email must be valid")
        @Size(max = 255)
        String email,

        @Size(max = 20)
        String status

) {}
