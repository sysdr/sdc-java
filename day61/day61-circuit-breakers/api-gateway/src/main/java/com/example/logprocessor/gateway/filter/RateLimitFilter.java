package com.example.logprocessor.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Extremely simple in-memory rate limiter (per source IP).
 * Production: replace with Redis-backed sliding window or token bucket.
 * Purpose here: demonstrate the gateway's gatekeeper role before
 * any downstream call is made.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final int MAX_REQUESTS_PER_WINDOW = 100;  // per 10-second window
    private static final long WINDOW_MS = 10_000;

    // ip -> [count, windowStart]
    private final ConcurrentHashMap<String, long[]> windows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {
        if (!request.getRequestURI().startsWith("/api/logs")) {
            chain.doFilter(request, response);
            return;
        }

        String ip = request.getRemoteAddr();
        long now = System.currentTimeMillis();

        long[] window = windows.computeIfAbsent(ip, k -> new long[]{0, now});

        synchronized (window) {
            if (now - window[1] > WINDOW_MS) {
                // Reset window
                window[0] = 1;
                window[1] = now;
            } else {
                window[0]++;
            }

            if (window[0] > MAX_REQUESTS_PER_WINDOW) {
                LOG.warn("Rate limit exceeded for IP: {}", ip);
                response.setStatus(429); // SC_TOO_MANY_REQUESTS
                response.getWriter().write("{\"error\":\"Rate limit exceeded. Retry after the current window.\"}");
                return;
            }
        }

        chain.doFilter(request, response);
    }
}
