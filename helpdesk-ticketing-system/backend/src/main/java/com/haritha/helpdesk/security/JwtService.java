package com.haritha.helpdesk.security;

import com.haritha.helpdesk.entity.Role;
import com.haritha.helpdesk.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("orgId", user.getOrganization().getId())
                .claim("role", user.getRole().name())
                .claim("email", user.getEmail())
                .claim("tokenVersion", user.getTokenVersion())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    /** Throws JwtException if the token is invalid or expired. */
    public AuthPrincipal parse(String token) {
        Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        Number organizationId = c.get("orgId", Number.class);
        Number tokenVersion = c.get("tokenVersion", Number.class);
        if (organizationId == null || tokenVersion == null) {
            throw new IllegalArgumentException("JWT is missing required claims");
        }
        return new AuthPrincipal(
                Long.valueOf(c.getSubject()),
                organizationId.longValue(),
                Role.valueOf((String) c.get("role")),
                (String) c.get("email"),
                tokenVersion.longValue());
    }
}
