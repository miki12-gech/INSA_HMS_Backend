package com.insa.hospital.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for settling the external hospital bill.
 *
 * Used by: PUT /api/referrals/{id}/settle-bill
 * Accessible by: ADMIN, FINANCE
 *
 * After settlement, the referral status transitions to COMPLETED,
 * indicating INSA has fully tracked the expense.
 */
public record ReferralBillingDto(

    /** Total amount billed by the external hospital (in ETB). */
    @NotNull(message = "External bill amount is required")
    @Positive(message = "Bill amount must be a positive number")
    Double externalBillAmount,

    /**
     * URL or file path of the uploaded invoice/bill document.
     * Optional — may be uploaded separately or not required for all facilities.
     */
    @Size(max = 1000)
    String billDocumentUrl

) {}
