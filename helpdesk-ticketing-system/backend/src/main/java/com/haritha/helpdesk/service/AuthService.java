package com.haritha.helpdesk.service;

import com.haritha.helpdesk.dto.Dtos.AuthResponse;
import com.haritha.helpdesk.dto.Dtos.LoginRequest;
import com.haritha.helpdesk.dto.Dtos.RegisterOrgRequest;
import com.haritha.helpdesk.entity.Organization;
import com.haritha.helpdesk.entity.Role;
import com.haritha.helpdesk.entity.User;
import com.haritha.helpdesk.exception.ConflictException;
import com.haritha.helpdesk.repository.OrganizationRepository;
import com.haritha.helpdesk.repository.UserRepository;
import com.haritha.helpdesk.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, OrganizationRepository organizationRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Creates a new organization together with its first admin user. */
    @Transactional
    public AuthResponse registerOrg(RegisterOrgRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email is already registered");
        }
        Organization org = new Organization();
        org.setName(req.orgName().trim());
        org = organizationRepository.save(org);

        User admin = new User();
        admin.setName(req.adminName().trim());
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(req.password()));
        admin.setRole(Role.ORG_ADMIN);
        admin.setOrganization(org);
        admin = userRepository.save(admin);
        return toResponse(admin);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmailIgnoreCase(req.email().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        return toResponse(user);
    }

    private AuthResponse toResponse(User u) {
        return new AuthResponse(jwtService.generateToken(u), u.getId(), u.getName(), u.getEmail(), u.getRole(),
                u.getOrganization().getId(), u.getOrganization().getName());
    }
}
