package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "medicine_issue_det")
@Data
public class MedicineIssueDet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "medicine_id")
    private String medicineId;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "receiver_quantity")
    private Integer receiverQuantity;

    @Column(name = "Remark")
    private String remark;

    @Column(name = "medicine_issue_Id")
    private Integer medicineIssueId;
}
