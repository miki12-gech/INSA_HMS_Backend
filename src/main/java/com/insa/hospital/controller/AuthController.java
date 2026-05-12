package com.insa.hospital.controller;

import com.insa.hospital.dto.EmployeeRegistrationRequest;
import com.insa.hospital.dto.EmployeeRegistrationResponse;
import com.insa.hospital.dto.LoginRequest;
import com.insa.hospital.dto.LoginResponse;
import com.insa.hospital.entity.Hospital;
import com.insa.hospital.entity.User;
import com.insa.hospital.repository.HospitalRepository;
import com.insa.hospital.repository.UserGroupRepository;
import com.insa.hospital.repository.UserRepository;
import com.insa.hospital.security.JwtService;
import com.insa.hospital.service.EmployeeApprovalService;
import com.insa.hospital.service.HospitalService;
import com.insa.hospital.service.TenantAccessService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Authentication Controller.
 *
 * Handles public auth endpoints for the legacy HMS system:
 *   - POST /api/auth/login
 *   - POST /api/auth/register
 *   - GET  /api/auth/me
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserGroupRepository userGroupRepository;
    private final HospitalRepository hospitalRepository;
    private final EmployeeApprovalService employeeApprovalService;
    private final HospitalService hospitalService;
    private final TenantAccessService tenantAccessService;

    @Autowired
    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          UserRepository userRepository,
                          UserGroupRepository userGroupRepository,
                          HospitalRepository hospitalRepository,
                          EmployeeApprovalService employeeApprovalService,
                          HospitalService hospitalService,
                          TenantAccessService tenantAccessService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.userGroupRepository = userGroupRepository;
        this.hospitalRepository = hospitalRepository;
        this.employeeApprovalService = employeeApprovalService;
        this.hospitalService = hospitalService;
        this.tenantAccessService = tenantAccessService;
    }

    /**
     * Login endpoint.
     *
     * Request body: { "email": "doctor@ni.com", "password": "secret" }
     * Success (200): LoginResponse with JWT token + user profile.
     * Failure (401): JSON error from GlobalExceptionHandler.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String loginIdentifier = request.email().trim();

        findUserForLogin(loginIdentifier).ifPresent(user -> {
            if (user.getActive() == null || user.getActive() != 1) {
                String role = userGroupRepository
                        .findGroupNameByUserId(user.getId())
                        .orElse("members");

                if ("Employee".equalsIgnoreCase(role)) {
                    throw new DisabledException("Account is pending approval");
                }

                throw new DisabledException("Account is not active");
            }
        });

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginIdentifier,
                        request.password()
                )
        );

        User user = findUserForLogin(loginIdentifier)
                .orElseThrow();

        String role = userGroupRepository
                .findGroupNameByUserId(user.getId())
                .orElse("members");
        String effectiveHospitalId = resolveEffectiveHospitalId(user, role);
        String effectiveHospitalName = resolveHospitalName(effectiveHospitalId, role);

        if (!tenantAccessService.isSuperAdminRole(role)
                && hospitalService.isHospitalUsageStopped(effectiveHospitalId)) {
            throw new DisabledException("Hospital access has been stopped by superadmin");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("hospitalId", effectiveHospitalId);
        claims.put("userId", String.valueOf(user.getId()));

        String token = jwtService.generateToken(user.getEmail(), claims);

        user.setLastLogin(System.currentTimeMillis() / 1000L);
        userRepository.save(user);

        return ResponseEntity.ok(
                LoginResponse.of(
                        token,
                        user.getId(),
                        user.getEmail(),
                        user.getUsername(),
                        role,
                        effectiveHospitalId,
                        effectiveHospitalName
                )
        );
    }

    /**
     * Public employee self-registration endpoint.
     *
     * Creates a new Employee user in inactive/pending state. An admin must later
     * approve the account before login is allowed.
     */
    @PostMapping("/register")
    public ResponseEntity<EmployeeRegistrationResponse> register(
            @Valid @RequestBody EmployeeRegistrationRequest request,
            HttpServletRequest httpRequest
    ) {
        EmployeeRegistrationResponse response = employeeApprovalService.registerPendingEmployee(
                request,
                httpRequest.getRemoteAddr()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Returns the currently authenticated user's profile from the JWT token.
     * Useful for the frontend to restore session state on page reload.
     *
     * GET /api/auth/me (requires valid JWT Bearer token)
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(Authentication authentication) {
        String email = authentication.getName();

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow();

        String role = userGroupRepository
                .findGroupNameByUserId(user.getId())
                .orElse("members");
        String effectiveHospitalId = resolveEffectiveHospitalId(user, role);
        String effectiveHospitalName = resolveHospitalName(effectiveHospitalId, role);

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("email", user.getEmail());
        profile.put("username", user.getUsername());
        profile.put("firstName", user.getFirstName());
        profile.put("lastName", user.getLastName());
        profile.put("role", role);
        profile.put("hospitalId", effectiveHospitalId);
        profile.put("hospitalName", effectiveHospitalName);
        profile.put("active", user.getActive());

        return ResponseEntity.ok(profile);
    }

    private String resolveEffectiveHospitalId(User user, String role) {
        if (StringUtils.hasText(user.getHospitalIonId())) {
            return tenantAccessService.normalizeHospitalScopeId(user.getHospitalIonId());
        }

        if ("admin".equalsIgnoreCase(role)
                && hospitalRepository.findByIonUserId(String.valueOf(user.getId())).isPresent()) {
            return tenantAccessService.normalizeHospitalScopeId(String.valueOf(user.getId()));
        }

        return tenantAccessService.getDefaultMainHospitalId();
    }

    private String resolveHospitalName(String hospitalId, String role) {
        if (tenantAccessService.isSuperAdminRole(role)) {
            return "All Branches";
        }

        if (!StringUtils.hasText(hospitalId)) {
            return "Hospital Workspace";
        }

        return hospitalRepository.findByIonUserId(hospitalId.trim())
                .map(Hospital::getName)
                .or(() -> {
                    try {
                        return hospitalRepository.findById(Integer.parseInt(hospitalId.trim()))
                                .map(Hospital::getName);
                    } catch (NumberFormatException ignored) {
                        return java.util.Optional.empty();
                    }
                })
                .orElse("Hospital Workspace");
    }

    private java.util.Optional<User> findUserForLogin(String loginIdentifier) {
        return userRepository.findByEmailIgnoreCase(loginIdentifier)
                .or(() -> userRepository.findByUsernameIgnoreCase(loginIdentifier));
    }
}
