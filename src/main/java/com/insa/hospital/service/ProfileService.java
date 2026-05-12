package com.insa.hospital.service;

import com.insa.hospital.dto.ProfileRequestDto;
import com.insa.hospital.dto.ProfileResponseDto;
import com.insa.hospital.entity.User;
import com.insa.hospital.repository.UserRepository;
import com.insa.hospital.security.JwtContextHolder;
import com.insa.hospital.util.StaffEmailPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public ProfileResponseDto updateProfile(ProfileRequestDto dto) {
        String ionUserIdStr = JwtContextHolder.getUserId();
        Long ionUserId = Long.parseLong(ionUserIdStr);

        User user = userRepository.findById(ionUserId)
                .orElseThrow(() -> new RuntimeException("User profile not found."));

        java.util.List<String> groups = jdbcTemplate.queryForList(
            "SELECT g.name FROM groups g JOIN users_groups ug ON g.id = ug.group_id WHERE ug.user_id = ? LIMIT 1",
            String.class, ionUserId);
        
        String groupName = groups.isEmpty() ? "members" : groups.get(0).toLowerCase();
        String normalizedEmail = StaffEmailPolicy.requiresInsaStaffEmail(groupName)
                ? StaffEmailPolicy.normalizeStaffEmail(dto.getEmail())
                : dto.getEmail().trim();

        // 1. Emulate legacy email check: "This Email Address Is Already Registered"
        if (!user.getEmail().equalsIgnoreCase(normalizedEmail)) {
            if (userRepository.existsByEmail(normalizedEmail)) {
                throw new RuntimeException("This Email Address Is Already Registered");
            }
        }

        // 2. Update core user credentials (legacy `updateIonUser`)
        // Legacy explicitly set parameter username as `name` from payload.
        user.setUsername(dto.getName());
        user.setEmail(normalizedEmail);
        if (StringUtils.hasText(dto.getPassword())) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        userRepository.save(user);

        // 3. Dynamic role table fallback update matching legacy strategy exactly
        if (!"superadmin".equals(groupName)) {
            if ("admin".equals(groupName)) {
                // Update hospital mapping directly
                String sql = "UPDATE hospital SET name = ?, email = ? WHERE ion_user_id = ?";
                jdbcTemplate.update(sql, dto.getName(), normalizedEmail, ionUserIdStr);
            } else {
                // Legacy did: $this->db->update($group_name, $data);
                // Validated mapping prevents SQL injection because groupNames are strictly defined in `groups` table
                // Typical values: doctor, patient, nurse, pharmacist, laboratorist, accountant, receptionist
                String sql = "UPDATE " + groupName + " SET name = ?, email = ? WHERE ion_user_id = ?";
                try {
                    jdbcTemplate.update(sql, dto.getName(), normalizedEmail, ionUserIdStr);
                } catch (Exception e) {
                    log.error("Failed to dynamically update profile for group {}: {}", groupName, e.getMessage());
                }
            }
        }

        return ProfileResponseDto.builder()
                .ionUserId(ionUserIdStr)
                .name(user.getUsername())
                .email(user.getEmail())
                .role(groupName)
                .hospitalId(user.getHospitalIonId())
                .build();
    }

    @Transactional(readOnly = true)
    public ProfileResponseDto getProfile() {
        String ionUserIdStr = JwtContextHolder.getUserId();
        Long ionUserId = Long.parseLong(ionUserIdStr);

        User user = userRepository.findById(ionUserId)
                .orElseThrow(() -> new RuntimeException("User profile not found."));

        java.util.List<String> groups = jdbcTemplate.queryForList(
            "SELECT g.name FROM groups g JOIN users_groups ug ON g.id = ug.group_id WHERE ug.user_id = ? LIMIT 1",
            String.class, ionUserId);
        
        String groupName = groups.isEmpty() ? "members" : groups.get(0).toLowerCase();

        return ProfileResponseDto.builder()
                .ionUserId(ionUserIdStr)
                .name(user.getUsername())
                .email(user.getEmail())
                .role(groupName)
                .hospitalId(user.getHospitalIonId())
                .build();
    }
}
