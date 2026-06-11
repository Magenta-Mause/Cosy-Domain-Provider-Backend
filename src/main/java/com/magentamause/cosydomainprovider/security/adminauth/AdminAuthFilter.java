package com.magentamause.cosydomainprovider.security.adminauth;

import com.magentamause.cosydomainprovider.configuration.admin.AdminProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class AdminAuthFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Admin-Key";

    private final AdminProperties properties;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String configuredKey = properties.getSecretKey();
        String providedKey = request.getHeader(HEADER_NAME);
        if (configuredKey == null
                || configuredKey.isBlank()
                || providedKey == null
                || !MessageDigest.isEqual(
                        configuredKey.getBytes(StandardCharsets.UTF_8),
                        providedKey.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid admin key\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
