package com.insa.hospital.dto;

import com.insa.hospital.entity.Medicine;

public record MedicineResponseDto(
    Integer id,
    String name,
    String category,
    String category1,
    String price,
    String sPrice,
    String box,
    Integer quantity,
    String generic,
    String company,
    String effects,
    String eDate,
    String strength,
    String addDate,
    String hospitalId
) {
    public static MedicineResponseDto from(Medicine m) {
        return new MedicineResponseDto(
            m.getId(),
            m.getName(),
            m.getCategory(),
            m.getCategory1(),
            m.getPrice(),
            m.getSPrice(),
            m.getBox(),
            m.getQuantity(),
            m.getGeneric(),
            m.getCompany(),
            m.getEffects(),
            m.getEDate(),
            m.getStrength(),
            m.getAddDate(),
            m.getHospitalId()
        );
    }
}
