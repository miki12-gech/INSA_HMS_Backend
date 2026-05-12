package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "service_charge")
@Getter
@Setter
@NoArgsConstructor
public class ServiceCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "hospital_id", length = 100, nullable = false)
    private String hospitalId;

    @Column(name = "visit_id", length = 100)
    private String visitId;

    @Column(name = "patient_id", length = 100, nullable = false)
    private String patientId;

    @Column(name = "patient_name", length = 255)
    private String patientName;

    @Column(name = "service_catalog_id")
    private Integer serviceCatalogId;

    @Column(name = "service_name", length = 255, nullable = false)
    private String serviceName;

    @Column(name = "service_group", length = 100)
    private String serviceGroup;

    @Column(name = "service_role", length = 100)
    private String serviceRole;

    @Column(name = "revenue_target", length = 100)
    private String revenueTarget;

    @Column(name = "responsible_doctor_id", length = 100)
    private String responsibleDoctorId;

    @Column(name = "responsible_doctor_name", length = 255)
    private String responsibleDoctorName;

    @Column(name = "ordering_staff_id", length = 100)
    private String orderingStaffId;

    @Column(name = "ordering_staff_name", length = 255)
    private String orderingStaffName;

    @Column(name = "ordering_staff_role", length = 100)
    private String orderingStaffRole;

    @Column(name = "unit_price", length = 100, nullable = false)
    private String unitPrice;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "total_amount", length = 100, nullable = false)
    private String totalAmount;

    @Column(name = "paid_amount", length = 100, nullable = false)
    private String paidAmount;

    @Column(name = "status", length = 50, nullable = false)
    private String status;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "charged_at", length = 100, nullable = false)
    private String chargedAt;

    @Column(name = "charged_date", length = 20, nullable = false)
    private String chargedDate;

    @Column(name = "created_at", length = 100, nullable = false)
    private String createdAt;

    @Column(name = "updated_at", length = 100, nullable = false)
    private String updatedAt;
}
