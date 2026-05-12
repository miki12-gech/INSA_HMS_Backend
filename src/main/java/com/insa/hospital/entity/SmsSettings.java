package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "sms_settings")
@Data
public class SmsSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String username;

    private String password;

    @Column(name = "api_id")
    private String apiId;

    private String sender;

    private String authkey;

    @Column(name = "\"user\"")
    private String user;

    @Column(name = "hospital_id")
    private String hospitalId;
}
