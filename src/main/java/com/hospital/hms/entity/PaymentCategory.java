package com.hospital.hms.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payment_category")
public class PaymentCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category")
    private String category;

    @Column(name = "description")
    private String description;

    @Column(name = "c_price")
    private String cPrice;

    @Column(name = "type")
    private String type;

    @Column(name = "d_commission")
    private Integer dCommission;

    @Column(name = "h_commission")
    private Integer hCommission;

    @Column(name = "hospital_id")
    private String hospitalId;
}
