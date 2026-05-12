package com.insa.hospital.controller;

import com.insa.hospital.dto.ReferralCreationDto;
import com.insa.hospital.dto.ReferralResponseDto;
import com.insa.hospital.entity.ReferralRequest.RequestedByRole;
import com.insa.hospital.security.JwtContextHolder;
import com.insa.hospital.service.ReferralService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Employee self-service referral endpoints.
 */
@RestController
@RequestMapping("/api/self-service")
public class SelfServiceController {

    private final ReferralService referralService;

    @Autowired
    public SelfServiceController(ReferralService referralService) {
        this.referralService = referralService;
    }

    @PostMapping("/referral")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'Employee')")
    public ResponseEntity<ReferralResponseDto> submitReferral(
            @Valid @RequestBody ReferralCreationDto dto) {

        String hospitalId = JwtContextHolder.getHospitalId();
        String userId = JwtContextHolder.getUserId();

        ReferralResponseDto created = referralService.requestReferral(
                dto, hospitalId, userId, RequestedByRole.EMPLOYEE);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/my-referrals")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'Employee')")
    public ResponseEntity<List<ReferralResponseDto>> getMyReferrals() {
        String hospitalId = JwtContextHolder.getHospitalId();
        String userId = JwtContextHolder.getUserId();

        return ResponseEntity.ok(referralService.getMyRequests(userId, hospitalId));
    }
}
