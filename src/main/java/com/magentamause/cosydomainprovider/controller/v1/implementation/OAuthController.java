package com.magentamause.cosydomainprovider.controller.v1.implementation;

import com.magentamause.cosydomainprovider.configuration.oauth.OAuthProperties;
import com.magentamause.cosydomainprovider.controller.v1.schema.OAuthApi;
import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.security.jwtfilter.JwtTokenBody;
import com.magentamause.cosydomainprovider.security.jwtfilter.JwtUtils;
import com.magentamause.cosydomainprovider.services.auth.AuthorizationService;
import com.magentamause.cosydomainprovider.services.auth.oauth.OAuthService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequiredArgsConstructor
public class OAuthController implements OAuthApi {

    private static final int MILLISECONDS_IN_SECOND = 1000;
    private static final String REFRESH_COOKIE_PATH = "/api/v1/auth/token";

    private final OAuthService oAuthService;
    private final AuthorizationService authorizationService;
    private final OAuthProperties oAuthProperties;
    private final JwtUtils jwtUtils;

    @Override
    public void authorize(String provider, HttpServletResponse response) throws IOException {
        String url = oAuthService.buildAuthorizationUrl(provider);
        response.sendRedirect(url);
    }

    @Override
    public void callback(String provider, String code, String state, HttpServletResponse response)
            throws IOException {
        try {
            UserEntity user = oAuthService.handleCallback(provider, code, state);
            String refreshToken = authorizationService.generateRefreshToken(user.getUuid());

            ResponseCookie cookie =
                    ResponseCookie.from("refreshToken", refreshToken)
                            .httpOnly(true)
                            .secure(oAuthProperties.isSecureCookie())
                            .maxAge(
                                    jwtUtils.getTokenValidityDuration(
                                                    JwtTokenBody.TokenType.REFRESH_TOKEN)
                                            / MILLISECONDS_IN_SECOND)
                            .path(REFRESH_COOKIE_PATH)
                            .sameSite("Lax")
                            .build();

            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
            response.sendRedirect(oAuthProperties.getFrontendUrl() + "/dashboard");
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("OAuth callback failed for provider {}", provider, e);
            response.sendRedirect(oAuthProperties.getFrontendUrl() + "/login?oauthError=true");
        }
    }
}
