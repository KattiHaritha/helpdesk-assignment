package com.haritha.helpdesk.service;

import com.haritha.helpdesk.dto.Dtos.ForgotPasswordRequest;
import com.haritha.helpdesk.dto.Dtos.MessageResponse;
import com.haritha.helpdesk.dto.Dtos.ResetPasswordRequest;
import com.haritha.helpdesk.entity.PasswordResetToken;
import com.haritha.helpdesk.entity.User;
import com.haritha.helpdesk.exception.BadRequestException;
import com.haritha.helpdesk.exception.EmailDeliveryException;
import com.haritha.helpdesk.repository.PasswordResetTokenRepository;
import com.haritha.helpdesk.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
public class PasswordResetService {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final long TOKEN_LIFETIME_SECONDS = 30 * 60;
    private static final String GENERIC_RESPONSE =
            "If an account exists for that email, a password reset link has been sent.";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final String frontendUrl;
    private final String fromAddress;
    private final String resendApiKey;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                PasswordEncoder passwordEncoder,
                                @Value("${app.frontend-url}") String frontendUrl,
                                @Value("${app.mail.from}") String fromAddress,
                                @Value("${resend.api-key:}") String resendApiKey) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.frontendUrl = frontendUrl;
        this.fromAddress = fromAddress;
        this.resendApiKey = resendApiKey;
    }

    @Transactional
    public MessageResponse requestReset(ForgotPasswordRequest request) {
        userRepository.findByEmailIgnoreCase(request.email().trim()).ifPresent(user -> {
            try {
                issueResetToken(user);
            } catch (EmailDeliveryException exception) {
                // Keep status and response identical for existing and unknown email addresses.
                log.warn("Password reset email delivery failed", exception);
            }
        });
        return new MessageResponse(GENERIC_RESPONSE);
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = tokenRepository.findByTokenHash(hashToken(request.token()))
                .filter(token -> token.getUsedAt() == null && token.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new BadRequestException("This password reset link is invalid or has expired."));

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);

        Instant now = Instant.now();
        resetToken.setUsedAt(now);
        tokenRepository.save(resetToken);

        List<PasswordResetToken> outstandingTokens =
                tokenRepository.findByUserIdAndUsedAtIsNull(user.getId());
        outstandingTokens.stream()
                .filter(token -> !token.getId().equals(resetToken.getId()))
                .forEach(token -> token.setUsedAt(now));
        tokenRepository.saveAll(outstandingTokens);

        return new MessageResponse("Your password has been reset. You can now sign in.");
    }

    private void issueResetToken(User user) {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        Instant now = Instant.now();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(hashToken(rawToken));
        resetToken.setExpiresAt(now.plusSeconds(TOKEN_LIFETIME_SECONDS));
        String resetLink = frontendUrl.replaceAll("/+$", "") + "/reset-password?token=" + rawToken;
        if (resendApiKey.isBlank() || fromAddress.isBlank()) {
            throw new EmailDeliveryException("Resend email settings are missing.");
        }

        CreateEmailOptions email = CreateEmailOptions.builder()
                .from(fromAddress)
                .to(user.getEmail())
                .subject("Reset your Helpdesk password")
                .html("""
                        <p>We received a request to reset your Helpdesk password.</p>
                        <p><a href="%s">Reset your password</a></p>
                        <p>This link expires in 30 minutes and can only be used once.</p>
                        <p>If you did not request this, you can ignore this email.</p>
                        """.formatted(resetLink))
                .build();
        try {
            new Resend(resendApiKey).emails().send(email);
        } catch (ResendException exception) {
            throw new EmailDeliveryException("Resend could not deliver the password reset email.", exception);
        }

        tokenRepository.findByUserIdAndUsedAtIsNull(user.getId())
                .forEach(existing -> existing.setUsedAt(now));
        tokenRepository.save(resetToken);
    }

    private static String hashToken(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
