package com.haritha.helpdesk.controller;

import com.haritha.helpdesk.dto.Dtos.CreateUserRequest;
import com.haritha.helpdesk.dto.Dtos.UserResponse;
import com.haritha.helpdesk.entity.Role;
import com.haritha.helpdesk.security.AuthPrincipal;
import com.haritha.helpdesk.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Create a user in your organization (admin only)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public UserResponse create(@Valid @RequestBody CreateUserRequest req, @AuthenticationPrincipal AuthPrincipal p) {
        return userService.create(req, p);
    }

    @Operation(summary = "List users of your organization (admin/agent), optional role filter")
    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN','AGENT')")
    public List<UserResponse> list(@RequestParam(required = false) Role role, @AuthenticationPrincipal AuthPrincipal p) {
        return userService.list(role, p);
    }
}
