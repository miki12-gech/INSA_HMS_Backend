package com.insa.hospital.dto;

import com.insa.hospital.entity.ReferralRequest;

/**
 * Response DTO for referral requests — returned to the frontend.
 *
 * Maps 1:1 from the {@link ReferralRequest} entity.
 * Uses a static factory method {@code from()} consistent with the
 * project's existing DTO conventions (e.g. PatientResponseDto).
 */
public record ReferralResponseDto(
    Long id,
    String patientId,
    String patientName,
    String requestedByRole,
    String requestedByUserId,
    String requestedByUserName,
    String destinationHospital,
    String reasonForReferral,
    String clinicalNotes,
    String status,
    String approvalDate,
    String approvedByUserId,
    String approvedByName,
    Double externalBillAmount,
    String billDocumentUrl,
    String letterManagementDocumentUrl,
    String letterManagementSentAt,
    String letterManagementSentByUserId,
    String letterManagementSentByName,
    String hospitalId,
    String createdAt
) {

    /**
     * Factory method: Entity → DTO (without resolved names).
     * Patient/requester names will be null; use the overloaded variant
     * when name resolution is available.
     */
    public static ReferralResponseDto from(ReferralRequest entity) {
        return from(entity, null, null, null);
    }

    /**
     * Factory method: Entity → DTO (with resolved names).
     *
     * @param entity           The JPA entity.
     * @param patientName      Resolved patient name (nullable).
     * @param requesterName    Resolved requester user name (nullable).
     */
    public static ReferralResponseDto from(ReferralRequest entity,
                                           String patientName,
                                           String requesterName,
                                           String letterManagementSentByName) {
        return new ReferralResponseDto(
            entity.getId(),
            entity.getPatientId(),
            patientName,
            entity.getRequestedByRole() != null
                ? entity.getRequestedByRole().name() : null,
            entity.getRequestedByUserId(),
            requesterName,
            entity.getDestinationHospital(),
            entity.getReasonForReferral(),
            entity.getClinicalNotes(),
            entity.getStatus() != null
                ? entity.getStatus().name() : null,
            entity.getApprovalDate(),
            entity.getApprovedByUserId(),
            entity.getApprovedByName(),
            entity.getExternalBillAmount(),
            entity.getBillDocumentUrl(),
            entity.getLetterManagementDocumentUrl(),
            entity.getLetterManagementSentAt(),
            entity.getLetterManagementSentByUserId(),
            letterManagementSentByName,
            entity.getHospitalId(),
            entity.getCreatedAt()
        );
    }
}
