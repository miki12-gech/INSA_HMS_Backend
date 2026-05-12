package com.insa.hospital.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PatientVisitStartRequestDto(

        @NotBlank(message = "Patient is required")
        @Size(max = 100)
        String patientId,

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

        @Size(max = 255)
        String assignedTo,

        @Size(max = 50)
        String assignedType,

        @Size(max = 50)
        String status,

        @Size(max = 50)
        String emergencyPriority,

        @Size(max = 100)
        String emergencyStage
) {
}
