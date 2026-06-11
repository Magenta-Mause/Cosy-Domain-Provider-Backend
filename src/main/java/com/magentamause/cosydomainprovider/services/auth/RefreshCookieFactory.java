package com.magentamause.cosydomainprovider.services.auth;

import com.magentamause.cosydomainprovider.configuration.security.AuthCookieProperties;
import com.magentamause.cosydomainprovider.security.jwtfilter.JwtTokenBody;
import com.magentamause.cosydomainprovider.security.jwtfilter.JwtUtils;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshCookieFactory {

    public static final String SAME_SITE_STRICT = "Strict";
    public static final String SAME_SITE_LAX = "Lax";

    private static final String COOKIE_NAME = "refreshToken";
    private static final String COOKIE_PATH = "/api/v1/auth/token";

    private final AuthCookieProperties authCookieProperties;
    private final JwtUtils jwtUtils;

    public ResponseCookie create(String refreshToken, String sameSite) {
        return builder(refreshToken, sameSite)
                .maxAge(
                        Duration.ofMillis(
                                jwtUtils.getTokenValidityDuration(
                                        JwtTokenBody.TokenType.REFRESH_TOKEN)))
                .build();
    }

    public ResponseCookie expire() {
        return builder("", SAME_SITE_STRICT).maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder builder(String value, String sameSite) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(authCookieProperties.isSecure())
                .path(COOKIE_PATH)
                .sameSite(sameSite);
    }
}
