package com.haritha.helpdesk.security;

import io.jsonwebtoken.JwtException;
import com.haritha.helpdesk.entity.User;
import com.haritha.helpdesk.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** Reads "Authorization: Bearer <jwt>", validates it and puts the user in the SecurityContext. */
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                AuthPrincipal principal = jwtService.parse(header.substring(7));
                User user = userRepository.findByIdAndOrganizationId(principal.userId(), principal.orgId())
                        .filter(current -> current.getTokenVersion() == principal.tokenVersion())
                        .filter(current -> current.getRole() == principal.role())
                        .orElseThrow(() -> new IllegalArgumentException("JWT has been revoked"));
                var auth = new UsernamePasswordAuthenticationToken(principal, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name())));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext(); // entry point will answer 401
            }
        }
        chain.doFilter(request, response);
    }
}
