package com.insa.hospital.dto;

import com.insa.hospital.entity.Doctor;

/**
 * Response DTO for doctor data returned to the frontend.
 */
public record DoctorResponseDto(
    Integer id,
    String name,
    String email,
    String address,
    String phone,
    String department,
    String profile,
    String imgUrl,
    String x,
    String y,
    String ionUserId,
    String hospitalId
) {
    public static DoctorResponseDto from(Doctor d) {
        return new DoctorResponseDto(
            d.getId(),
            d.getName(),
            d.getEmail(),
            d.getAddress(),
            d.getPhone(),
            d.getDepartment(),
            d.getProfile(),
            d.getImgUrl(),
            d.getX(),
            d.getY(),
            d.getIonUserId(),
            d.getHospitalId()
        );
    }
}
