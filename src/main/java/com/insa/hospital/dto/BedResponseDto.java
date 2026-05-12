package com.insa.hospital.dto;

import com.insa.hospital.entity.Bed;

public record BedResponseDto(
    Long id,
    String category,
    String number,
    String description,
    String lastATime,
    String lastDTime,
    /** null or blank = available; 'occupied' = occupied */
    String status,
    /** Composite: "category-number" (e.g. 'Icu-1') */
    String bedId,
    String hospitalId,
    /** Computed convenience field */
    boolean available
) {
    public static BedResponseDto from(Bed b) {
        boolean avail = b.getStatus() == null || b.getStatus().isBlank();
        return new BedResponseDto(
            b.getId(), b.getCategory(), b.getNumber(), b.getDescription(),
            b.getLastATime(), b.getLastDTime(), b.getStatus(),
            b.getBedId(), b.getHospitalId(), avail
        );
    }
}
