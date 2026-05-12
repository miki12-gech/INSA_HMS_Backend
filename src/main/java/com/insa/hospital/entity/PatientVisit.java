package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "patient_visit")
@Getter
@Setter
@NoArgsConstructor
public class PatientVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "patient_id", length = 100, nullable = false)
    private String patientId;

    @Column(name = "reason_for_visit", length = 500)
    private String reasonForVisit;

    @Column(name = "status", length = 50, nullable = false)
    private String status;

    @Column(name = "visit_type", length = 50, nullable = false)
    private String visitType;

    @Column(name = "assigned_department_id", length = 100)
    private String assignedDepartmentId;

    @Column(name = "assigned_department_name", length = 255)
    private String assignedDepartmentName;

    @Column(name = "assigned_doctor_id", length = 100)
    private String assignedDoctorId;

    @Column(name = "assigned_doctor_name", length = 255)
    private String assignedDoctorName;

    @Column(name = "assigned_nurse_id", length = 100)
    private String assignedNurseId;

    @Column(name = "assigned_nurse_name", length = 255)
    private String assignedNurseName;

    @Column(name = "doctor_id", length = 100)
    private String doctorId;

    @Column(name = "doctor_name", length = 255)
    private String doctorName;

    @Column(name = "nurse_id", length = 100)
    private String nurseId;

    @Column(name = "nurse_name", length = 255)
    private String nurseName;

    @Column(name = "department_id", length = 100)
    private String departmentId;

    @Column(name = "department_name", length = 255)
    private String departmentName;

    @Column(name = "prescription_id", length = 100)
    private String prescriptionId;

    @Column(name = "lab_order_id", length = 100)
    private String labOrderId;

    @Column(name = "assigned_to", length = 255)
    private String assignedTo;

    @Column(name = "assigned_type", length = 50)
    private String assignedType;

    @Column(name = "emergency_priority", length = 50)
    private String emergencyPriority;

    @Column(name = "emergency_assessment", length = 4000)
    private String emergencyAssessment;

    @Column(name = "emergency_treatment", length = 4000)
    private String emergencyTreatment;

    @Column(name = "emergency_disposition", length = 255)
    private String emergencyDisposition;

    @Column(name = "emergency_disposition_note", length = 2000)
    private String emergencyDispositionNote;

    @Column(name = "emergency_stage", length = 100)
    private String emergencyStage;

    @Column(name = "emergency_stage_note", length = 2000)
    private String emergencyStageNote;

    @Column(name = "emergency_medication_summary", length = 4000)
    private String emergencyMedicationSummary;

    @Column(name = "emergency_billing_amount", length = 100)
    private String emergencyBillingAmount;

    @Column(name = "emergency_billing_note", length = 2000)
    private String emergencyBillingNote;

    @Column(name = "emergency_referral_destination", length = 500)
    private String emergencyReferralDestination;

    @Column(name = "emergency_follow_up_instruction", length = 2000)
    private String emergencyFollowUpInstruction;

    @Column(name = "emergency_completed_at", length = 100)
    private String emergencyCompletedAt;

    @Column(name = "created_at", length = 100, nullable = false)
    private String createdAt;

    @Column(name = "updated_at", length = 100, nullable = false)
    private String updatedAt;

    @Column(name = "hospital_id", length = 100, nullable = false)
    private String hospitalId;
}
