package com.insa.hospital.dto;

import lombok.Data;

@Data
public class HospitalRequestDto {
    private Integer id;
    private String name;
    private String password;
    private String email;
    private String address;
    private String phone;
    private String packageId; // named 'package' in DB, mapped to hospitalPackage
    private String p_limit;
    private String d_limit;
    private String[] module; // from frontend, it's an array 
    private String language; // requested in addNew method
}
