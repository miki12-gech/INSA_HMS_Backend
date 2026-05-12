package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "pharmacy_expense")
@Data
public class PharmacyExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category")
    private String category;

    @Column(name = "date")
    private String date;

    @Column(name = "amount")
    private String amount;

    @Column(name = "\"user\"")
    private String user;

    @Column(name = "hospital_id")
    private String hospitalId;
}
