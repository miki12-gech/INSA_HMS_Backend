package com.insa.hospital.dto;

import com.insa.hospital.entity.Hospital;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class HospitalResponseDto {
    private Integer id;
    private String name;
    private String email;
    private String address;
    private String phone;
    private String packageId; // 'package' in db
    private String p_limit;
    private String d_limit;
    private String module;
    private String ion_user_id;
    private String usageStatus;

    public HospitalResponseDto(Hospital hospital) {
        this.id = hospital.getId();
        this.name = hospital.getName();
        this.email = hospital.getEmail();
        this.address = hospital.getAddress();
        this.phone = hospital.getPhone();
        this.packageId = hospital.getHospitalPackage();
        this.p_limit = hospital.getPLimit();
        this.d_limit = hospital.getDLimit();
        this.module = hospital.getModule();
        this.ion_user_id = hospital.getIonUserId();
        this.usageStatus = "Active";
    }
}
