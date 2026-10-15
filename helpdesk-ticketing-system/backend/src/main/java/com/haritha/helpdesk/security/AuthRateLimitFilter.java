package com.haritha.helpdesk.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Per-process limits for public account endpoints. Use a shared store when running multiple instances. */
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private record Policy(int maximum, long windowMillis) {}
    private static final class Window {
        private final long startedAt;
        private int count;
        private Window(long startedAt) { this.startedAt = startedAt; this.count = 1; }
    }

    private static final long MINUTE = Duration.ofMinutes(1).toMillis();
    private static final long HOUR = Duration.ofHours(1).toMillis();
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final AtomicLong requests = new AtomicLong();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Policy policy = policyFor(request);
        if (policy == null) {
            chain.doFilter(request, response);
            return;
        }

        long now = System.currentTimeMillis();
        String key = request.getRemoteAddr() + ':' + request.getRequestURI();
        Window window = windows.compute(key, (ignored, current) -> {
            if (current == null || now - current.startedAt >= policy.windowMillis()) return new Window(now);
            synchronized (current) { current.count++; }
            return current;
        });
        boolean allowed;
        synchronized (window) { allowed = window.count <= policy.maximum(); }
        if (!allowed) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", Long.toString(Math.max(1,
                    (policy.windowMillis() - (now - window.startedAt) + 999) / 1000)));
            response.getWriter().write("{\"status\":429,\"message\":\"Too many requests. Try again later.\",\"errors\":[]}");
            return;
        }

        if (requests.incrementAndGet() % 512 == 0) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().startedAt >= policy.windowMillis());
        }
        chain.doFilter(request, response);
    }

    private static Policy policyFor(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) return null;
        return switch (request.getRequestURI()) {
            case "/api/auth/login" -> new Policy(10, MINUTE);
            case "/api/auth/register-org", "/api/auth/forgot-password" -> new Policy(5, HOUR);
            case "/api/auth/reset-password" -> new Policy(10, HOUR);
            default -> null;
        };
    }
}
