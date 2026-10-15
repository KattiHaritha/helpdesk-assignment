package com.haritha.helpdesk.controller;

import com.haritha.helpdesk.dto.Dtos.AuthResponse;
import com.haritha.helpdesk.dto.Dtos.ForgotPasswordRequest;
import com.haritha.helpdesk.dto.Dtos.LoginRequest;
import com.haritha.helpdesk.dto.Dtos.MessageResponse;
import com.haritha.helpdesk.dto.Dtos.RegisterOrgRequest;
import com.haritha.helpdesk.dto.Dtos.ResetPasswordRequest;
import com.haritha.helpdesk.service.AuthService;
import com.haritha.helpdesk.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth")
public class AuthController {
    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    @Operation(summary = "Create a new organization and its first admin")
    @PostMapping("/register-org")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerOrg(@Valid @RequestBody RegisterOrgRequest req) {
        return authService.registerOrg(req);
    }

    @Operation(summary = "Login and get a JWT")
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @Operation(summary = "Request a password reset email")
    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        return passwordResetService.requestReset(req);
    }

    @Operation(summary = "Reset password using a one-time email token")
    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        return passwordResetService.resetPassword(req);
    }
}
