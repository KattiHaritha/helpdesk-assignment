package com.haritha.helpdesk.security;

import com.haritha.helpdesk.entity.Role;

/** Logged-in user info taken from the JWT. orgId here is the ONLY source of tenant identity. */
public record AuthPrincipal(Long userId, Long orgId, Role role, String email, long tokenVersion) {
    public AuthPrincipal(Long userId, Long orgId, Role role, String email) {
        this(userId, orgId, role, email, 0);
    }

    public boolean isStaff() {
        return role == Role.ORG_ADMIN || role == Role.AGENT;
    }
}
