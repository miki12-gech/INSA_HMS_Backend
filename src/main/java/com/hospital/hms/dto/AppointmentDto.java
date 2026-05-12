package com.hospital.hms.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentDto {
    private Long id;
    private String patient;
    private String doctor;
    private String date;
    private String timeSlot;
    private String sTime;
    private String eTime;
    private String remarks;
    private String status;
    private String request;
    private String patientCategory;
    
    private String sms;
    private String redirect;

    // Extra fields if "patient" == "add_new"
    private String pName;
    private String pEmail;
    private String pPhone;
    private String pAge;
    private String pGender;
    
    // Auth bypass context (to mimic legacy user=ion_id)
    private String user;
}
