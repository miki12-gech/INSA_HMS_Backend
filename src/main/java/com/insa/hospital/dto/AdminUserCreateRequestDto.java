package com.insa.hospital.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for superadmin staff account creation.
 *
 * Legacy persistence strategy:
 * - Auth credentials are stored in the ion_auth `users` and `users_groups` tables.
 * - departmentId is persisted in users.company for staff IAM purposes because the
 *   legacy auth table does not have a dedicated foreign key column for department.
 */
public record AdminUserCreateRequestDto(

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be 100 characters or fewer")
    String name,

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email must be 100 characters or fewer")
    String email,

    @NotBlank(message = "Phone is required")
    @Size(max = 20, message = "Phone must be 20 characters or fewer")
    String phone,

    @NotBlank(message = "Role is required")
    @Size(max = 50, message = "Role must be 50 characters or fewer")
    String role,

    @NotBlank(message = "Hospital is required")
    @Size(max = 100, message = "Hospital must be 100 characters or fewer")
    String hospitalId,

    @NotNull(message = "Department is required")
    Integer departmentId
) {}
