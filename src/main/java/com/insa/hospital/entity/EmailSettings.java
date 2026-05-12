package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "email_settings")
@Data
public class EmailSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_email")
    private String adminEmail;

    private String type;

    @Column(name = "\"user\"")
    private String user;

    private String password;

    @Column(name = "hospital_id")
    private String hospitalId;
}
