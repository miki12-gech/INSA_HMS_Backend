package com.insa.hospital.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PatientVisitStatusUpdateRequestDto(

        @Size(max = 100)
        String visitId,

        @Size(max = 100)
        String patientId,

        @NotBlank(message = "New status is required")
        @Size(max = 50)
        String newStatus,

        @Size(max = 500)
        String reasonForVisit,

        @Size(max = 50)
        String visitType,

        @Size(max = 100)
        String assignedDepartmentId,

        @Size(max = 255)
        String assignedDepartmentName,

        @Size(max = 100)
        String assignedDoctorId,

        @Size(max = 255)
        String assignedDoctorName,

        @Size(max = 100)
        String assignedNurseId,

        @Size(max = 255)
        String assignedNurseName,

        @Size(max = 100)
        String doctorId,

        @Size(max = 255)
        String doctorName,

        @Size(max = 100)
        String nurseId,

        @Size(max = 255)
        String nurseName,

        @Size(max = 100)
        String departmentId,

        @Size(max = 255)
        String departmentName,

        @Size(max = 100)
        String prescriptionId,

        @Size(max = 100)
        String labOrderId,

        @Size(max = 50)
        String emergencyPriority,

        @Size(max = 4000)
        String emergencyAssessment,

        @Size(max = 4000)
        String emergencyTreatment,

        @Size(max = 255)
        String emergencyDisposition,

        @Size(max = 2000)
        String emergencyDispositionNote,

        @Size(max = 100)
        String emergencyStage,

        @Size(max = 2000)
        String emergencyStageNote,

        @Size(max = 4000)
        String emergencyMedicationSummary,

        @Size(max = 100)
        String emergencyBillingAmount,

        @Size(max = 2000)
        String emergencyBillingNote,

        @Size(max = 500)
        String emergencyReferralDestination,

        @Size(max = 2000)
        String emergencyFollowUpInstruction
) {
}
