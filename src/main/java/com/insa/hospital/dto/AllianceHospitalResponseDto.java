package com.insa.hospital.dto;

import com.insa.hospital.entity.AllianceHospital;

import java.time.LocalDateTime;

public record AllianceHospitalResponseDto(
        Long id,
        String name,
        String address,
        String phone,
        String email,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static AllianceHospitalResponseDto from(AllianceHospital hospital) {
        return new AllianceHospitalResponseDto(
                hospital.getId(),
                hospital.getName(),
                hospital.getAddress(),
                hospital.getPhone(),
                hospital.getEmail(),
                hospital.getStatus(),
                hospital.getCreatedAt(),
                hospital.getUpdatedAt()
        );
    }
}
