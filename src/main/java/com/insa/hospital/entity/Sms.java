package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "sms")
@Data
public class Sms {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String date;

    private String message;

    private String recipient;

    @Column(name = "\"user\"")
    private String user;

    @Column(name = "hospital_id")
    private String hospitalId;
}
