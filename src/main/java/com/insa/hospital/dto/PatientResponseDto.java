package com.insa.hospital.dto;

import com.insa.hospital.entity.Patient;

/**
 * Response DTO for patient data returned to the frontend.
 * Maps cleanly from the Patient entity without exposing raw internal fields.
 */
public record PatientResponseDto(
    Integer id,
    String name,
    String email,
    String phone,
    String address,
    String sex,
    String birthdate,
    String age,
    String bloodgroup,
    String patientId,
    String addDate,
    String registrationTime,
    String hospitalId,
    String membershiptype,
    Integer depId,
    String allergynote,
    String doctor,
    String imgUrl,
    String howAdded,
    String ionUserId
) {
    /**
     * Factory method — maps a Patient entity to this DTO.
     * Called from the service layer; never exposes the entity directly.
     */
    public static PatientResponseDto from(Patient p) {
        return new PatientResponseDto(
            p.getId(),
            p.getName(),
            p.getEmail(),
            p.getPhone(),
            p.getAddress(),
            p.getSex(),
            p.getBirthdate(),
            p.getAge(),
            p.getBloodgroup(),
            p.getPatientId(),
            p.getAddDate(),
            p.getRegistrationTime(),
            p.getHospitalId(),
            p.getMembershiptype(),
            p.getDepId(),
            p.getAllergynote(),
            p.getDoctor(),
            p.getImgUrl(),
            p.getHowAdded(),
            p.getIonUserId()
        );
    }
}
