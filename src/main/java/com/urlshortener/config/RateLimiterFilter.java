package com.urlshortener.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class RateLimiterFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 10000000;
    private final ConcurrentMap<String, List<Long>> ipRequestTimestamps = new ConcurrentHashMap<>();

    @org.springframework.beans.factory.annotation.Value("${app.rate-limiting.enabled:true}")
    private boolean enabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        
        // Exclude Swagger UI, H2 Console, and OpenAPI documentation from rate limiting
        if (path.startsWith("/swagger-ui") || 
            path.startsWith("/v3/api-docs") || 
            path.startsWith("/h2-console") || 
            path.equals("/favicon.ico")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        long now = Instant.now().toEpochMilli();
        long windowStart = now - 60000; // 1 minute window

        // Retrieve or initialize the sliding window list for the IP
        List<Long> timestamps = ipRequestTimestamps.computeIfAbsent(clientIp, k -> new CopyOnWriteArrayList<>());

        boolean allowed = false;
        synchronized (timestamps) {
            // Remove timestamps older than 1 minute
            timestamps.removeIf(t -> t < windowStart);
            
            if (timestamps.size() < MAX_REQUESTS_PER_MINUTE) {
                timestamps.add(now);
                allowed = true;
            }
        }

        if (!allowed) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                String.format(
                    "{\"timestamp\":\"%s\",\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Max %d requests per minute.\",\"path\":\"%s\"}",
                    Instant.now().toString(),
                    MAX_REQUESTS_PER_MINUTE,
                    path
                )
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
