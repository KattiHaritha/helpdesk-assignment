package com.haritha.helpdesk.dto;

import com.haritha.helpdesk.entity.HistoryType;
import com.haritha.helpdesk.entity.Priority;
import com.haritha.helpdesk.entity.Role;
import com.haritha.helpdesk.entity.Status;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;

/** All request/response DTOs in one place. Entities are never returned directly. */
public final class Dtos {
    private Dtos() {}

    // ---------- auth ----------
    public record RegisterOrgRequest(
            @NotBlank @Size(max = 100) String orgName,
            @NotBlank @Size(max = 100) String adminName,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 100) String password) {}

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    public record ForgotPasswordRequest(@NotBlank @Email String email) {}

    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8, max = 100) String password) {}

    public record MessageResponse(String message) {}

    public record AuthResponse(String token, Long userId, String name, String email, Role role,
                               Long organizationId, String organizationName) {}

    // ---------- users ----------
    public record CreateUserRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotNull Role role) {}

    public record UserResponse(Long id, String name, String email, Role role, Long organizationId) {}

    public record UserSummary(Long id, String name) {}

    // ---------- tickets ----------
    public record CreateTicketRequest(
            @NotBlank @Size(max = 200) String subject,
            @NotBlank @Size(max = 4000) String description,
            Priority priority) {}

    public record UpdateTicketRequest(
            @NotBlank @Size(max = 200) String subject,
            @NotBlank @Size(max = 4000) String description,
            @NotNull Priority priority) {}

    public record StatusRequest(@NotNull Status status) {}

    /** agentId = null means "unassign" */
    public record AssignRequest(Long agentId) {}

    public record TicketResponse(Long id, String subject, String description, Priority priority, Status status,
                                 UserSummary requester, UserSummary assignedAgent, Long organizationId,
                                 Instant createdAt, Instant updatedAt) {}

    // ---------- comments & history ----------
    public record CommentRequest(@NotBlank @Size(max = 4000) String content, boolean internal) {}

    public record CommentResponse(Long id, Long ticketId, UserSummary author, String content,
                                  boolean internal, Instant createdAt) {}

    public record HistoryResponse(Long id, HistoryType type, String oldValue, String newValue,
                                  UserSummary changedBy, Instant createdAt) {}

    // ---------- misc ----------
    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        public static <T> PageResponse<T> of(Page<T> p) {
            return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
        }
    }

    public record TicketSummaryResponse(long total, long open, long inProgress, long resolved, long closed) {}

    public record ErrorResponse(Instant timestamp, int status, String message, List<String> errors) {}
}
