package com.insa.hospital.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating or updating a doctor profile.
 * hospital_id and ion_user_id are resolved server-side from JWT context.
 */
public record DoctorRequestDto(

    @NotBlank(message = "Doctor name is required")
    @Size(max = 100)
    String name,

    @Email(message = "Must be a valid email")
    @Size(max = 100)
    String email,

    @Size(max = 100)
    String address,

    @Size(max = 100)
    String phone,

    /** Medical specialty or department name. */
    @Size(max = 100)
    String department,

    /** Short profile/bio text. */
    @Size(max = 100)
    String profile,

    /** Profile image URL (optional, if uploaded externally). */
    @Size(max = 100)
    String imgUrl,

    /** Legacy x column — passed through as-is. */
    @Size(max = 100)
    String x,

    /** Legacy y column — passed through as-is. */
    @Size(max = 10)
    String y,

    /**
     * ion_user_id to link this doctor profile to a users.id account.
     * Required when creating a new doctor with a login account.
     */
    @Size(max = 100)
    String ionUserId,

    /**
     * Password required when creating a new doctor account via the admin panel.
     */
    @Size(max = 100)
    String password

) {}
