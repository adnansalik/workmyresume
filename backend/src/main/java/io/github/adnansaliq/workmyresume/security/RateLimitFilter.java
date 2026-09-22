package io.github.adnansaliq.workmyresume.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory fixed-window rate limiter for sensitive paths (default /auth/*).
 * In-memory is single-instance only; a distributed limiter (e.g. Redis) would be
 * required with multiple app instances.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final class Window {
        final long startNanos;
        int count;

        Window(long startNanos, int count) {
            this.startNanos = startNanos;
            this.count = count;
        }
    }

    private final int maxRequests;
    private final long windowNanos;
    private final List<String> limitedPathPrefixes;
    private final ConcurrentHashMap<String, Window> counters = new ConcurrentHashMap<>();

    public RateLimitFilter(
            @Value("${app.rate-limit.max-requests:300}") int maxRequests,
            @Value("${app.rate-limit.window-seconds:60}") long windowSeconds,
            @Value("${app.rate-limit.paths:/auth/}") String limitedPaths) {
        this.maxRequests = maxRequests;
        this.windowNanos = windowSeconds * 1_000_000_000L;
        this.limitedPathPrefixes = List.of(limitedPaths.split(","));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return limitedPathPrefixes.stream().noneMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String key = clientIp(request);
        long now = System.nanoTime();

        Window window = counters.compute(key, (k, existing) -> {
            if (existing == null || now - existing.startNanos > windowNanos) {
                return new Window(now, 1);
            }
            existing.count++;
            return existing;
        });

        if (window.count > maxRequests) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"message\":\"Too many requests - try again shortly\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}