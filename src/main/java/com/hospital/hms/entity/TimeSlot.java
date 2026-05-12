package com.hospital.hms.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "time_slot")
public class TimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doctor")
    private String doctor;

    @Column(name = "s_time")
    private String sTime;

    @Column(name = "e_time")
    private String eTime;

    @Column(name = "weekday")
    private String weekday;

    @Column(name = "s_time_key")
    private String sTimeKey;

    @Column(name = "hospital_id")
    private String hospitalId;
}
