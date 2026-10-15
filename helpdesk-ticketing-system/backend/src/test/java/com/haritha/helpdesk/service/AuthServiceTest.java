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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock OrganizationRepository organizationRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @InjectMocks AuthService authService;

    private User existingUser() {
        Organization org = new Organization();
        org.setId(1L);
        org.setName("Acme");
        User u = new User();
        u.setId(7L);
        u.setName("Jane");
        u.setEmail("jane@acme.com");
        u.setPasswordHash("hash");
        u.setRole(Role.AGENT);
        u.setOrganization(org);
        return u;
    }

    @Test
    void login_wrongPassword_throwsBadCredentials() {
        when(userRepository.findByEmailIgnoreCase("jane@acme.com")).thenReturn(Optional.of(existingUser()));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThrows(BadCredentialsException.class,
                () -> authService.login(new LoginRequest("jane@acme.com", "wrong")));
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void login_unknownEmail_throwsBadCredentials() {
        when(userRepository.findByEmailIgnoreCase("nobody@x.com")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class,
                () -> authService.login(new LoginRequest("nobody@x.com", "whatever")));
    }

    @Test
    void login_success_returnsTokenAndOrganization() {
        User u = existingUser();
        when(userRepository.findByEmailIgnoreCase("jane@acme.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("secret1", "hash")).thenReturn(true);
        when(jwtService.generateToken(u)).thenReturn("jwt-token");

        AuthResponse res = authService.login(new LoginRequest("jane@acme.com", "secret1"));

        assertEquals("jwt-token", res.token());
        assertEquals(1L, res.organizationId());
        assertEquals(Role.AGENT, res.role());
    }

    @Test
    void registerOrg_duplicateEmail_throwsConflict() {
        when(userRepository.existsByEmailIgnoreCase("jane@acme.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.registerOrg(
                new RegisterOrgRequest("Acme", "Jane", "jane@acme.com", "secret1")));
        verify(organizationRepository, never()).save(any());
    }
}
