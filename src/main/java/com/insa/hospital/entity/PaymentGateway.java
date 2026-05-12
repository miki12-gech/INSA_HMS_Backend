package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "paymentgateway")
@Data
@NoArgsConstructor
public class PaymentGateway {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "merchant_key", length = 100)
    private String merchantKey;

    @Column(name = "salt", length = 100)
    private String salt;

    @Column(name = "x", length = 100)
    private String x;

    @Column(name = "y", length = 100)
    private String y;

    @Column(name = "APIUsername", length = 100)
    private String apiUsername;

    @Column(name = "APIPassword", length = 100)
    private String apiPassword;

    @Column(name = "APISignature", length = 100)
    private String apiSignature;

    @Column(name = "status", length = 1000)
    private String status;

    @Column(name = "hospital_id", length = 100)
    private String hospitalId;
}
