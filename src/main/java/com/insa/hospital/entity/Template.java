package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "template")
@Getter
@Setter
public class Template {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String template;

    private String category;
    @Column(name = "\"user\"")
    private String user;

    @Column(name = "hospital_id")
    private String hospitalId;
}
