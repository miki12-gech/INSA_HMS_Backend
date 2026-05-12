package com.insa.hospital.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for Admin to approve or reject a referral.
 *
 * Used by: PUT /api/referrals/{id}/approve
 * Accessible by: ADMIN, SUPERADMIN
 */
public record ReferralApprovalDto(

    /**
     * The decision: "APPROVED" or "REJECTED".
     * Must be a valid ReferralStatus enum name.
     */
    @NotNull(message = "Status decision is required (APPROVED or REJECTED)")
    String status,

    /**
     * Optional remarks from the admin.
     * For rejections, this should contain the reason.
     * Appended to the referral's clinicalNotes field.
     */
    String adminRemarks

) {}
