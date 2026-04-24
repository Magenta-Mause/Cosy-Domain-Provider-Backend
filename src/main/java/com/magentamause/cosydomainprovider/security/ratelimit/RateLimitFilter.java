package com.magentamause.cosydomainprovider.security.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.magentamause.cosydomainprovider.configuration.ratelimit.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String AUTH_PATH_PREFIX = "/api/v1/auth/";

    private final RateLimitProperties properties;
    private final Cache<String, Bucket> authBuckets =
            Caffeine.newBuilder().expireAfterAccess(Duration.ofHours(1)).build();
    private final Cache<String, Bucket> globalBuckets =
            Caffeine.newBuilder().expireAfterAccess(Duration.ofHours(1)).build();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String ip = resolveClientIp(request);
        boolean isAuthPath = request.getRequestURI().startsWith(AUTH_PATH_PREFIX);

        Bucket bucket =
                isAuthPath
                        ? authBuckets.get(ip, k -> newBucket(properties.auth()))
                        : globalBuckets.get(ip, k -> newBucket(properties.global()));

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests\"}");
        }
    }

    private Bucket newBucket(RateLimitProperties.Rule rule) {
        Bandwidth limit =
                Bandwidth.classic(
                        rule.requests(),
                        Refill.greedy(rule.requests(), Duration.ofSeconds(rule.periodSeconds())));
        return Bucket.builder().addLimit(limit).build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
