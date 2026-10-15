package com.haritha.helpdesk.service;

import com.haritha.helpdesk.dto.Dtos.CreateUserRequest;
import com.haritha.helpdesk.dto.Dtos.UserResponse;
import com.haritha.helpdesk.entity.Organization;
import com.haritha.helpdesk.entity.Role;
import com.haritha.helpdesk.entity.User;
import com.haritha.helpdesk.exception.ConflictException;
import com.haritha.helpdesk.exception.ResourceNotFoundException;
import com.haritha.helpdesk.repository.OrganizationRepository;
import com.haritha.helpdesk.repository.UserRepository;
import com.haritha.helpdesk.security.AuthPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, OrganizationRepository organizationRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** New user always lands in the caller's own organization. */
    @Transactional
    public UserResponse create(CreateUserRequest req, AuthPrincipal p) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email is already registered");
        }
        Organization org = organizationRepository.findById(p.orgId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        User u = new User();
        u.setName(req.name().trim());
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setRole(req.role());
        u.setOrganization(org);
        return toResponse(userRepository.save(u));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list(Role role, AuthPrincipal p) {
        List<User> users = role == null
                ? userRepository.findByOrganizationIdOrderByNameAsc(p.orgId())
                : userRepository.findByOrganizationIdAndRoleOrderByNameAsc(p.orgId(), role);
        return users.stream().map(UserService::toResponse).toList();
    }

    private static UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getOrganization().getId());
    }
}
