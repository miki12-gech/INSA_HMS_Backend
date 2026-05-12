package com.insa.hospital.service;

import com.insa.hospital.dto.ReferralApprovalDto;
import com.insa.hospital.dto.ReferralBillingDto;
import com.insa.hospital.dto.ReferralCreationDto;
import com.insa.hospital.dto.ReferralResponseDto;
import com.insa.hospital.entity.ReferralRequest;
import com.insa.hospital.entity.ReferralRequest.ReferralStatus;
import com.insa.hospital.entity.ReferralRequest.RequestedByRole;
import com.insa.hospital.exception.BadRequestException;
import com.insa.hospital.exception.ResourceNotFoundException;
import com.insa.hospital.repository.PatientRepository;
import com.insa.hospital.repository.ReferralRequestRepository;
import com.insa.hospital.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReferralService {

    private static final Logger log = LoggerFactory.getLogger(ReferralService.class);

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final ReferralRequestRepository referralRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;

    @Autowired
    public ReferralService(ReferralRequestRepository referralRepository,
                           PatientRepository patientRepository,
                           UserRepository userRepository) {
        this.referralRepository = referralRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
    }

    public ReferralResponseDto requestReferral(ReferralCreationDto dto,
                                               String hospitalId,
                                               String userId) {
        return requestReferral(dto, hospitalId, userId, null);
    }

    /**
     * Creates a new referral request in PENDING status.
     *
     * When {@code requestedByRole} is provided explicitly, it overrides the
     * payload value. This lets the self-service controller force EMPLOYEE as the
     * origin without trusting client input.
     */
    public ReferralResponseDto requestReferral(ReferralCreationDto dto,
                                               String hospitalId,
                                               String userId,
                                               RequestedByRole requestedByRole) {

        RequestedByRole role = requestedByRole != null
                ? requestedByRole
                : parseRequestedByRole(dto.requestedByRole());

        ReferralRequest referral = new ReferralRequest();
        referral.setPatientId(dto.patientId());
        referral.setRequestedByRole(role);
        referral.setRequestedByUserId(userId);
        referral.setDestinationHospital(dto.destinationHospital());
        referral.setReasonForReferral(resolveReasonForReferral(dto.reasonForReferral(), role));
        referral.setClinicalNotes(dto.clinicalNotes());
        referral.setStatus(ReferralStatus.PENDING);
        referral.setHospitalId(hospitalId);
        referral.setCreatedAt(LocalDateTime.now().format(DATETIME_FMT));

        ReferralRequest saved = referralRepository.save(referral);
        log.info("Referral #{} created by {} [role={}] for patient {} -> {}",
                saved.getId(), userId, role, dto.patientId(), dto.destinationHospital());

        return toResponseDto(saved);
    }

    public ReferralResponseDto approveReferral(Long referralId,
                                               ReferralApprovalDto dto,
                                               String hospitalId,
                                               String approverUserId) {

        ReferralRequest referral = findReferralById(referralId, hospitalId);

        if (referral.getStatus() != ReferralStatus.PENDING) {
            throw new BadRequestException(
                    "Referral #" + referralId + " cannot be approved/rejected - current status is "
                            + referral.getStatus() + ". Only PENDING referrals can be processed.");
        }

        ReferralStatus newStatus;
        try {
            newStatus = ReferralStatus.valueOf(dto.status().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BadRequestException(
                    "Invalid status decision: '" + dto.status()
                            + "'. Must be 'APPROVED' or 'REJECTED'.");
        }

        if (newStatus != ReferralStatus.APPROVED && newStatus != ReferralStatus.REJECTED) {
            throw new BadRequestException(
                    "Invalid status decision: '" + dto.status()
                            + "'. Only APPROVED or REJECTED are valid for this operation.");
        }

        referral.setStatus(newStatus);
        referral.setApprovalDate(LocalDate.now().format(DATE_FMT));
        referral.setApprovedByUserId(approverUserId);
        referral.setApprovedByName(resolveUserDisplayName(approverUserId));

        if (StringUtils.hasText(dto.adminRemarks())) {
            String existingNotes = referral.getClinicalNotes();
            String separator = StringUtils.hasText(existingNotes) ? "\n\n" : "";
            String prefix = newStatus == ReferralStatus.REJECTED
                    ? "[REJECTION REASON] "
                    : "[ADMIN REMARKS] ";
            referral.setClinicalNotes(
                    (existingNotes != null ? existingNotes : "")
                            + separator + prefix + dto.adminRemarks()
            );
        }

        ReferralRequest updated = referralRepository.save(referral);
        log.info("Referral #{} {} by admin (hospitalId={})",
                referralId, newStatus, hospitalId);

        return toResponseDto(updated);
    }

    public ReferralResponseDto settleExternalBill(Long referralId,
                                                  ReferralBillingDto dto,
                                                  String hospitalId) {

        ReferralRequest referral = findReferralById(referralId, hospitalId);

        if (referral.getStatus() != ReferralStatus.APPROVED) {
            throw new BadRequestException(
                    "Referral #" + referralId + " cannot be settled - current status is "
                            + referral.getStatus()
                            + ". Only APPROVED referrals can have their bills settled.");
        }

        referral.setExternalBillAmount(dto.externalBillAmount());
        if (StringUtils.hasText(dto.billDocumentUrl())) {
            referral.setBillDocumentUrl(dto.billDocumentUrl());
        }
        referral.setStatus(ReferralStatus.COMPLETED);

        ReferralRequest updated = referralRepository.save(referral);
        log.info("Referral #{} bill settled - amount={} ETB (hospitalId={})",
                referralId, dto.externalBillAmount(), hospitalId);

        return toResponseDto(updated);
    }

    public ReferralResponseDto sendToLetterManagement(Long referralId,
                                                      MultipartFile file,
                                                      String note,
                                                      String hospitalId,
                                                      String senderUserId) throws IOException {

        ReferralRequest referral = findReferralById(referralId, hospitalId);

        if (referral.getRequestedByRole() != RequestedByRole.EMPLOYEE) {
            throw new BadRequestException(
                    "Only employee medical requests can be sent to letter management.");
        }

        if (referral.getStatus() != ReferralStatus.APPROVED
                && referral.getStatus() != ReferralStatus.COMPLETED) {
            throw new BadRequestException(
                    "Referral #" + referralId + " must be approved before sending to letter management.");
        }

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("A document file is required.");
        }

        referral.setLetterManagementDocumentUrl(storeLetterManagementDocument(file));
        referral.setLetterManagementSentAt(LocalDateTime.now().format(DATETIME_FMT));
        referral.setLetterManagementSentByUserId(senderUserId);

        if (StringUtils.hasText(note)) {
            appendAdminNote(referral, "[LETTER MANAGEMENT NOTE] ", note.trim());
        }

        ReferralRequest updated = referralRepository.save(referral);
        log.info("Referral #{} sent to letter management by {} (hospitalId={})",
                referralId, senderUserId, hospitalId);

        return toResponseDto(updated);
    }

    @Transactional(readOnly = true)
    public List<ReferralResponseDto> getPendingReferrals(String hospitalId) {
        List<ReferralRequest> results;
        if (StringUtils.hasText(hospitalId)) {
            results = referralRepository
                    .findByStatusAndHospitalIdOrderByIdDesc(ReferralStatus.PENDING, hospitalId);
        } else {
            results = referralRepository.findByStatusOrderByIdDesc(ReferralStatus.PENDING);
        }
        return results.stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReferralResponseDto> getMyRequests(String userId, String hospitalId) {
        return referralRepository
                .findByRequestedByUserIdAndHospitalIdOrderByIdDesc(userId, hospitalId)
                .stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReferralResponseDto> getPatientReferrals(String patientId, String hospitalId) {
        return referralRepository
                .findByPatientIdAndHospitalIdOrderByIdDesc(patientId, hospitalId)
                .stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReferralResponseDto> getAllReferrals(String hospitalId) {
        List<ReferralRequest> results;
        if (StringUtils.hasText(hospitalId)) {
            results = referralRepository.findByHospitalIdOrderByIdDesc(hospitalId);
        } else {
            results = referralRepository.findAllByOrderByIdDesc();
        }
        return results.stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReferralResponseDto getReferralById(Long id, String hospitalId) {
        ReferralRequest referral = findReferralById(id, hospitalId);
        return toResponseDto(referral);
    }

    private ReferralRequest findReferralById(Long referralId, String hospitalId) {
        if (StringUtils.hasText(hospitalId)) {
            return referralRepository
                    .findByIdAndHospitalId(referralId, hospitalId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "ReferralRequest", "id", referralId));
        }

        return referralRepository
                .findById(referralId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ReferralRequest", "id", referralId));
    }

    private ReferralResponseDto toResponseDto(ReferralRequest entity) {
        String requesterName = resolveRequesterName(entity.getRequestedByUserId());
        String patientName = resolvePatientName(entity.getPatientId());
        String letterManagementSentByName =
                resolveUserDisplayName(entity.getLetterManagementSentByUserId());

        if (entity.getRequestedByRole() == RequestedByRole.EMPLOYEE
                && StringUtils.hasText(requesterName)
                && StringUtils.hasText(entity.getPatientId())
                && entity.getPatientId().contains("@")) {
            patientName = requesterName;
        }

        return ReferralResponseDto.from(
                entity,
                patientName,
                requesterName,
                letterManagementSentByName
        );
    }

    private String storeLetterManagementDocument(MultipartFile file) throws IOException {
        Path uploadsDir = Paths.get("uploads", "referrals").toAbsolutePath().normalize();
        Files.createDirectories(uploadsDir);

        String originalFilename = Optional.ofNullable(file.getOriginalFilename())
                .orElse("letter-management-document");
        String cleanedFilename = org.springframework.util.StringUtils.cleanPath(originalFilename);
        int extensionIndex = cleanedFilename.lastIndexOf('.');
        String extension = extensionIndex >= 0 ? cleanedFilename.substring(extensionIndex) : "";
        String fileName = UUID.randomUUID() + extension;

        Path targetPath = uploadsDir.resolve(fileName).normalize();
        file.transferTo(targetPath);
        return "uploads/referrals/" + fileName;
    }

    private void appendAdminNote(ReferralRequest referral, String prefix, String value) {
        String existingNotes = referral.getClinicalNotes();
        String separator = StringUtils.hasText(existingNotes) ? "\n\n" : "";
        referral.setClinicalNotes((existingNotes != null ? existingNotes : "") + separator + prefix + value);
    }

    private String resolvePatientName(String patientIdStr) {
        if (!StringUtils.hasText(patientIdStr)) {
            return null;
        }

        try {
            int pk = Integer.parseInt(patientIdStr.trim());
            return patientRepository.findById(pk)
                    .map(p -> p.getName())
                    .orElse(patientIdStr);
        } catch (NumberFormatException e) {
            return patientIdStr;
        }
    }

    private RequestedByRole parseRequestedByRole(String requestedByRole) {
        try {
            return RequestedByRole.valueOf(requestedByRole.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BadRequestException(
                    "Invalid requestedByRole: '" + requestedByRole
                            + "'. Must be 'DOCTOR' or 'EMPLOYEE'.");
        }
    }

    private String resolveRequesterName(String requestedByUserId) {
        return resolveUserDisplayName(requestedByUserId);
    }

    private String resolveUserDisplayName(String userIdValue) {
        if (!StringUtils.hasText(userIdValue)) {
            return null;
        }

        try {
            Long userId = Long.parseLong(userIdValue.trim());
            return userRepository.findById(userId)
                    .map(user -> {
                        String fullName = ((user.getFirstName() != null ? user.getFirstName() : "")
                                + " "
                                + (user.getLastName() != null ? user.getLastName() : "")).trim();
                        if (StringUtils.hasText(fullName)) {
                            return fullName;
                        }

                        if (StringUtils.hasText(user.getUsername())) {
                            return user.getUsername();
                        }

                        return user.getEmail();
                    })
                    .orElse(null);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String resolveReasonForReferral(String rawReason,
                                            RequestedByRole requestedByRole) {
        if (requestedByRole == RequestedByRole.EMPLOYEE) {
            return StringUtils.hasText(rawReason)
                    ? rawReason.trim()
                    : "Employee medical request";
        }

        if (!StringUtils.hasText(rawReason)) {
            throw new BadRequestException("Reason for referral is required for doctor referrals.");
        }

        return rawReason.trim();
    }
}
