package com.haritha.helpdesk.service;

import com.haritha.helpdesk.dto.Dtos.ResetPasswordRequest;
import com.haritha.helpdesk.entity.PasswordResetToken;
import com.haritha.helpdesk.entity.User;
import com.haritha.helpdesk.repository.PasswordResetTokenRepository;
import com.haritha.helpdesk.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PasswordResetServiceTest {
    @Test
    void resetPasswordIncrementsVersionToRevokeExistingSessions() {
        UserRepository users = mock(UserRepository.class);
        PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        User user = new User();
        user.setTokenVersion(4);
        PasswordResetToken token = new PasswordResetToken();
        token.setId(9L);
        token.setUser(user);
        token.setExpiresAt(Instant.now().plusSeconds(300));
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(tokens.findByUserIdAndUsedAtIsNull(null)).thenReturn(List.of(token));
        when(encoder.encode("a-new-password-123")).thenReturn("encoded-password");

        PasswordResetService service = new PasswordResetService(users, tokens, encoder,
                "http://localhost:5173", "", "");
        service.resetPassword(new ResetPasswordRequest("valid-token", "a-new-password-123"));

        assertEquals(5, user.getTokenVersion());
        assertEquals("encoded-password", user.getPasswordHash());
        verify(users).save(user);
        verify(tokens).save(token);
    }
}
