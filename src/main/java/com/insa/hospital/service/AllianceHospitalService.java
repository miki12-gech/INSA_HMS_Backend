package com.insa.hospital.service;

import com.insa.hospital.dto.AllianceHospitalRequestDto;
import com.insa.hospital.dto.AllianceHospitalResponseDto;
import com.insa.hospital.entity.AllianceHospital;
import com.insa.hospital.exception.BadRequestException;
import com.insa.hospital.repository.AllianceHospitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class AllianceHospitalService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_INACTIVE = "INACTIVE";

    private final AllianceHospitalRepository allianceHospitalRepository;

    @Transactional(readOnly = true)
    public List<AllianceHospitalResponseDto> listHospitals(String hospitalId) {
        List<AllianceHospital> hospitals = StringUtils.hasText(hospitalId)
                ? allianceHospitalRepository.findByHospitalIdOrderByNameAsc(hospitalId.trim())
                : allianceHospitalRepository.findAllByOrderByNameAsc();

        return hospitals.stream()
                .map(AllianceHospitalResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AllianceHospitalResponseDto> listActiveHospitals(String hospitalId) {
        List<AllianceHospital> hospitals = StringUtils.hasText(hospitalId)
                ? allianceHospitalRepository.findByHospitalIdAndStatusOrderByNameAsc(
                        hospitalId.trim(), STATUS_ACTIVE)
                : allianceHospitalRepository.findByStatusOrderByNameAsc(STATUS_ACTIVE);

        return hospitals.stream()
                .map(AllianceHospitalResponseDto::from)
                .toList();
    }

    public AllianceHospitalResponseDto addHospital(AllianceHospitalRequestDto dto, String hospitalId) {
        if (!StringUtils.hasText(hospitalId)) {
            throw new BadRequestException("Missing hospital scope for alliance hospital management.");
        }

        LocalDateTime now = LocalDateTime.now();

        AllianceHospital hospital = new AllianceHospital();
        hospital.setHospitalId(hospitalId.trim());
        hospital.setName(dto.name().trim());
        hospital.setAddress(dto.address().trim());
        hospital.setPhone(dto.phone().trim());
        hospital.setEmail(StringUtils.hasText(dto.email()) ? dto.email().trim() : null);
        hospital.setStatus(normalizeStatus(dto.status()));
        hospital.setCreatedAt(now);
        hospital.setUpdatedAt(now);

        return AllianceHospitalResponseDto.from(allianceHospitalRepository.save(hospital));
    }

    private String normalizeStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return STATUS_ACTIVE;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (STATUS_ACTIVE.equals(normalized) || STATUS_INACTIVE.equals(normalized)) {
            return normalized;
        }

        throw new BadRequestException("Invalid hospital status. Use ACTIVE or INACTIVE.");
    }
}
