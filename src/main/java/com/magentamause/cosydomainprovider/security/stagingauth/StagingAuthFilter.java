package com.magentamause.cosydomainprovider.security.stagingauth;

import com.magentamause.cosydomainprovider.configuration.stagingauth.StagingAuthProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class StagingAuthFilter extends OncePerRequestFilter {

    static final String COOKIE_NAME = "STAGING_AUTH";
    static final String AUTH_PATH = "/api/v1/staging-auth";

    private final StagingAuthProperties properties;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!properties.enabled()) {
            chain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        if (AUTH_PATH.equals(path)) {
            handleAuthEndpoint(request, response);
            return;
        }

        if (hasValidStagingCookie(request)) {
            chain.doFilter(request, response);
            return;
        }

        writeUnauthorized(response, "Staging authentication required");
    }

    private void handleAuthEndpoint(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if (HttpMethod.GET.matches(request.getMethod())) {
            if (hasValidStagingCookie(request)) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("application/json");
                response.getWriter().write("{}");
            } else {
                writeUnauthorized(response, "Staging authentication required");
            }
            return;
        }

        if (HttpMethod.POST.matches(request.getMethod())) {
            String[] credentials = resolveBasicCredentials(request);
            if (credentials != null
                    && credentials[0].equals(properties.username())
                    && MessageDigest.isEqual(
                            credentials[1].getBytes(StandardCharsets.UTF_8),
                            properties.password().getBytes(StandardCharsets.UTF_8))) {
                setStagingCookie(response);
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("application/json");
                response.getWriter().write("{}");
            } else {
                writeUnauthorized(response, "Invalid staging credentials");
            }
            return;
        }

        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    private boolean hasValidStagingCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return false;
        String expected = computeCookieValue();
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return MessageDigest.isEqual(
                        cookie.getValue().getBytes(StandardCharsets.UTF_8),
                        expected.getBytes(StandardCharsets.UTF_8));
            }
        }
        return false;
    }

    private void setStagingCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(COOKIE_NAME, computeCookieValue());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(604800); // 7 days
        response.addCookie(cookie);
    }

    private String computeCookieValue() {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(
                    new SecretKeySpec(
                            properties.password().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hmac = mac.doFinal(properties.username().getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmac);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute staging cookie value", e);
        }
    }

    private String[] resolveBasicCredentials(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Basic ")) return null;
        try {
            String decoded =
                    new String(
                            Base64.getDecoder().decode(header.substring(6)),
                            StandardCharsets.UTF_8);
            int colon = decoded.indexOf(':');
            if (colon < 0) return null;
            return new String[] {decoded.substring(0, colon), decoded.substring(colon + 1)};
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
