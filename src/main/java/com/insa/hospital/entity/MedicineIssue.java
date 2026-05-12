package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "medicine_issue")
@Data
public class MedicineIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_id")
    private String categoryId;

    @Column(name = "medicine_id")
    private String medicineId;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "receiver_quantity")
    private Integer receiverQuantity;

    @Column(name = "status")
    private String status;

    @Column(name = "receiver_name")
    private String receiverName;

    @Column(name = "\"user\"")
    private String user;

    @Column(name = "date")
    private String date;

    @Column(name = "date_string")
    private String dateString;

    @Column(name = "datetime_string")
    private String datetimeString;

    @Column(name = "hospital_id")
    private String hospitalId;
}
