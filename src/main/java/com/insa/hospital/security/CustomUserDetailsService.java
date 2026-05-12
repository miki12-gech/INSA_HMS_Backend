package com.insa.hospital.security;

import com.insa.hospital.entity.User;
import com.insa.hospital.repository.UserGroupRepository;
import com.insa.hospital.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * Custom UserDetailsService that loads users from the legacy `users` table
 * and resolves their role from the `users_groups` + `groups` tables.
 *
 * Spring Security calls loadUserByUsername(email) during authentication.
 * We use email as the "username" since the legacy system allows login by email.
 *
 * Role mapping:
 *   The group name (e.g. "Doctor", "Nurse") is prefixed with "ROLE_" to follow
 *   Spring Security conventions. The JWT token also carries this raw group name.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserGroupRepository userGroupRepository;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository,
                                    UserGroupRepository userGroupRepository) {
        this.userRepository = userRepository;
        this.userGroupRepository = userGroupRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String loginIdentifier) throws UsernameNotFoundException {
        // Load user from the legacy `users` table
        User user = userRepository.findByEmailIgnoreCase(loginIdentifier)
                .or(() -> userRepository.findByUsernameIgnoreCase(loginIdentifier))
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with login identifier: " + loginIdentifier));

        // Resolve the role before the active-state check so pending Employee accounts
        // can return a precise approval message at login time.
        String roleName = userGroupRepository
                .findGroupNameByUserId(user.getId())
                .orElse("members");

        // Verify the account is active (active=1 in legacy schema)
        if (user.getActive() == null || user.getActive() != 1) {
            if ("Employee".equalsIgnoreCase(roleName)) {
                throw new DisabledException("Account is pending approval");
            }
            throw new DisabledException("Account is not active");
        }

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + roleName);

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(Collections.singletonList(authority))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
}
