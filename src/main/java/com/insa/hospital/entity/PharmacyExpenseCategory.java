package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "pharmacy_expense_category")
@Data
public class PharmacyExpenseCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category")
    private String category;

    @Column(name = "description")
    private String description;

    @Column(name = "x")
    private String x;

    @Column(name = "y")
    private String y;

    @Column(name = "hospital_id")
    private String hospitalId;
}
