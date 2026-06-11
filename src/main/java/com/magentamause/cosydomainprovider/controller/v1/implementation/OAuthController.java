package com.magentamause.cosydomainprovider.controller.v1.implementation;

import com.magentamause.cosydomainprovider.configuration.web.FrontendProperties;
import com.magentamause.cosydomainprovider.controller.v1.schema.OAuthApi;
import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.security.jwtfilter.JwtTokenBody;
import com.magentamause.cosydomainprovider.security.jwtfilter.JwtUtils;
import com.magentamause.cosydomainprovider.services.auth.AuthorizationService;
import com.magentamause.cosydomainprovider.services.auth.RefreshCookieFactory;
import com.magentamause.cosydomainprovider.services.auth.oauth.OAuthService;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequiredArgsConstructor
public class OAuthController implements OAuthApi {

    private final OAuthService oAuthService;
    private final AuthorizationService authorizationService;
    private final FrontendProperties frontendProperties;
    private final RefreshCookieFactory refreshCookieFactory;
    private final JwtUtils jwtUtils;

    @Override
    public ResponseEntity<Void> authorize(String provider) {
        String url = oAuthService.buildAuthorizationUrl(provider);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }

    @Override
    public ResponseEntity<Void> callback(String provider, String code, String state) {
        try {
            String linkedUserId = oAuthService.peekLinkedUserId(state);
            if (linkedUserId != null) {
                return handleLinkCallback(provider, code, state);
            }
            return handleLoginCallback(provider, code, state);
        } catch (Exception e) {
            log.error("OAuth callback failed for provider {}", provider, e);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendProperties.getUrl() + "/login?oauthError=true"))
                    .build();
        }
    }

    private ResponseEntity<Void> handleLoginCallback(String provider, String code, String state) {
        try {
            UserEntity user = oAuthService.handleLoginCallback(provider, code, state);

            if (user.isMfaEnabled()) {
                String challengeToken =
                        jwtUtils.generateToken(JwtTokenBody.forMfaChallengeToken(user));
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(
                                URI.create(
                                        frontendProperties.getUrl()
                                                + "/mfa-challenge?token="
                                                + challengeToken))
                        .build();
            }

            String refreshToken = authorizationService.generateRefreshToken(user.getUuid());
            ResponseCookie cookie =
                    refreshCookieFactory.create(refreshToken, RefreshCookieFactory.SAME_SITE_LAX);

            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .location(URI.create(frontendProperties.getUrl() + "/dashboard"))
                    .build();
        } catch (ResponseStatusException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(
                                URI.create(
                                        frontendProperties.getUrl()
                                                + "/login?oauthError=emailTaken"))
                        .build();
            }
            throw e;
        }
    }

    private ResponseEntity<Void> handleLinkCallback(String provider, String code, String state) {
        try {
            oAuthService.handleLinkCallback(provider, code, state);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendProperties.getUrl() + "/settings?linked=true"))
                    .build();
        } catch (ResponseStatusException e) {
            log.warn("OAuth link failed for provider {}: {}", provider, e.getReason());
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendProperties.getUrl() + "/settings?linkError=true"))
                    .build();
        }
    }
}
