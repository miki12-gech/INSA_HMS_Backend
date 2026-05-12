package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity mapping the legacy `medicine` table.
 *
 * Column mapping from nhospital1 SQL dump (lines 899–917).
 * ALL 17 columns mapped 1:1 — DO NOT add, rename, or remove columns.
 *
 * Key observations from actual data rows (lines 924–937):
 *  - category  : legacy category name (joins to medicine_category.category)
 *  - category1 : sub-category (joins to medicine_category1.category)
 *  - e_date    : expiry date in "DD-MM-YYYY" or "MM/DD/YYYY" format (String)
 *  - e_date_n  : expiry date as Unix epoch string (numeric string)
 *  - add_date  : "MM/DD/YY" format
 *  - quantity  : int — actual stock count. Only int column in the entity.
 *  - s_price   : selling price (String: "210", "2")
 *  - price     : purchase/cost price
 *  - box       : units per box
 *  - user      : int — references users.id of who added this medicine
 */
@Entity
@Table(name = "medicine")
@Getter
@Setter
@NoArgsConstructor
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", length = 100)
    private String name;

    /** Category name (links to medicine_category.category). */
    @Column(name = "category", length = 100)
    private String category;

    /** Purchase/cost price per unit. */
    @Column(name = "price", length = 100)
    private String price;

    /** Units per box. */
    @Column(name = "box", length = 100)
    private String box;

    /** Selling price per unit. */
    @Column(name = "s_price", length = 100)
    private String sPrice;

    /** Current stock quantity (actual int column in legacy schema). */
    @Column(name = "quantity")
    private Integer quantity;

    /** Generic name / brand. */
    @Column(name = "generic", length = 100)
    private String generic;

    /** Manufacturer/company. */
    @Column(name = "company", length = 100)
    private String company;

    /** Side effects or notes. */
    @Column(name = "effects", length = 100)
    private String effects;

    /** Expiry date in legacy text format (e.g. "28-02-2022" or "11/01/2021"). */
    @Column(name = "e_date", length = 70)
    private String eDate;

    /** Date medicine was added ("MM/DD/YY"). */
    @Column(name = "add_date", length = 100)
    private String addDate;

    /** Multi-tenant hospital identifier. */
    @Column(name = "hospital_id", length = 100)
    private String hospitalId;

    /** Sub-category (links to medicine_category1.category). */
    @Column(name = "category1", length = 50)
    private String category1;

    /** Strength/dosage unit (e.g. "400mg", "232"). */
    @Column(name = "strength", length = 50)
    private String strength;

    /** Expiry date as Unix epoch string. */
    @Column(name = "e_date_n", length = 50)
    private String eDateN;

    /** users.id of who added this medicine (int, nullable). */
    @Column(name = "\"user\"")
    private Integer user;
}
