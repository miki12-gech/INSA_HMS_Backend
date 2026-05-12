package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity mapping the legacy `payment_category` table.
 *
 * Column mapping from nhospital1 SQL dump (lines 1499–1507).
 * ALL 8 columns mapped 1:1.
 *
 * This is the fee/service catalogue used to populate billing dropdowns.
 * Key columns:
 *  - c_price       : default charge/price for this category
 *  - type          : 'diagnostic' | 'others' (affects commission split)
 *  - d_commission  : doctor commission percentage (int)
 *  - h_commission  : hospital commission percentage (int)
 *
 * Example from data:
 *   (36, 'CBC (DIGITAL)', 'Pathological Test', '450', 'diagnostic', 30, 0, '416')
 */
@Entity
@Table(name = "payment_category")
@Getter
@Setter
@NoArgsConstructor
public class PaymentCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /** Service/item name (e.g. "CBC (DIGITAL)", "E.C.G"). */
    @Column(name = "category", length = 100)
    private String category;

    /** Description of this fee category. */
    @Column(name = "description", length = 100)
    private String description;

    /** Default charge price (String, e.g. "450"). */
    @Column(name = "c_price", length = 100)
    private String cPrice;

    /** Category type — 'diagnostic' | 'others'. */
    @Column(name = "type", length = 100)
    private String type;

    /** Doctor commission percentage (e.g. 30 → 30%). */
    @Column(name = "d_commission")
    private Integer dCommission;

    /** Hospital commission percentage (e.g. 0 → not applicable). */
    @Column(name = "h_commission")
    private Integer hCommission;

    /** Multi-tenant hospital identifier. */
    @Column(name = "hospital_id", length = 100)
    private String hospitalId;

    /** Functional grouping for reporting: Consultation, Lab, Procedure, etc. */
    @Column(name = "service_group", length = 100)
    private String serviceGroup;

    /** Clinical or operational owner of the service: Doctor, Nurse, Laboratory, etc. */
    @Column(name = "service_role", length = 100)
    private String serviceRole;

    /** Revenue destination used in rollup reports. */
    @Column(name = "revenue_target", length = 100)
    private String revenueTarget;

    /** Catalog visibility for charge capture forms. */
    @Column(name = "active")
    private Boolean active = Boolean.TRUE;
}
