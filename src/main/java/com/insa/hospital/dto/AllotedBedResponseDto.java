package com.insa.hospital.dto;

import com.insa.hospital.entity.AllotedBed;

public record AllotedBedResponseDto(
    Long id,
    String patient,
    String bedId,
    String aTime,
    String dTime,
    /** '1' = active/occupied; '0' = discharged. */
    String status,
    String hospitalId
) {
    public static AllotedBedResponseDto from(AllotedBed a) {
        return new AllotedBedResponseDto(
            a.getId(), a.getPatient(), a.getBedId(),
            a.getATime(), a.getDTime(), a.getStatus(), a.getHospitalId()
        );
    }
}
