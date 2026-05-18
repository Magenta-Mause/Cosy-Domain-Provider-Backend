package com.magentamause.cosydomainprovider.controller.v1;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.magentamause.cosydomainprovider.configuration.oauth.OAuthProperties;
import com.magentamause.cosydomainprovider.controller.v1.implementation.OAuthController;
import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.security.jwtfilter.JwtTokenBody;
import com.magentamause.cosydomainprovider.security.jwtfilter.JwtUtils;
import com.magentamause.cosydomainprovider.services.auth.AuthorizationService;
import com.magentamause.cosydomainprovider.services.auth.oauth.OAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OAuthControllerTest {

    @Mock private OAuthService oAuthService;
    @Mock private AuthorizationService authorizationService;
    @Mock private OAuthProperties oAuthProperties;
    @Mock private JwtUtils jwtUtils;

    private OAuthController controller;

    @BeforeEach
    void setUp() {
        controller =
                new OAuthController(oAuthService, authorizationService, oAuthProperties, jwtUtils);
        when(oAuthProperties.getFrontendUrl()).thenReturn("http://localhost:5173");
        when(jwtUtils.getTokenValidityDuration(JwtTokenBody.TokenType.REFRESH_TOKEN))
                .thenReturn(2_678_400_000L);
        when(oAuthService.peekLinkedUserId(anyString())).thenReturn(null);
    }

    @Test
    void authorize_redirectsToAuthUrl() {
        when(oAuthService.buildAuthorizationUrl("github"))
                .thenReturn("https://github.com/login/oauth/authorize?client_id=abc");

        ResponseEntity<Void> resp = controller.authorize("github");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(resp.getHeaders().getLocation().toString())
                .contains("github.com/login/oauth/authorize");
    }

    @Test
    void callback_noMfa_setsCookieAndRedirects() {
        UserEntity user =
                UserEntity.builder().uuid("u1").username("alice").isMfaEnabled(false).build();
        when(oAuthService.handleLoginCallback("github", "code123", "state123")).thenReturn(user);
        when(authorizationService.generateRefreshToken("u1")).thenReturn("rt");

        ResponseEntity<Void> resp = controller.callback("github", "code123", "state123");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(resp.getHeaders().getFirst("Set-Cookie")).contains("refreshToken");
        assertThat(resp.getHeaders().getLocation().toString()).contains("/dashboard");
    }

    @Test
    void callback_mfaEnabled_redirectsToMfaChallenge() {
        UserEntity user =
                UserEntity.builder().uuid("u1").username("alice").isMfaEnabled(true).build();
        when(oAuthService.handleLoginCallback("github", "code123", "state123")).thenReturn(user);
        when(jwtUtils.generateToken(any())).thenReturn("challenge-token");

        ResponseEntity<Void> resp = controller.callback("github", "code123", "state123");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(resp.getHeaders().getLocation().toString()).contains("/mfa-challenge");
    }

    @Test
    void callback_responseStatusException_redirectsToLoginWithError() {
        when(oAuthService.handleLoginCallback(any(), any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid state"));

        ResponseEntity<Void> resp = controller.callback("github", "code", "state");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(resp.getHeaders().getLocation().toString()).contains("oauthError=true");
    }

    @Test
    void callback_unexpectedException_redirectsToLoginWithError() {
        when(oAuthService.handleLoginCallback(any(), any(), any()))
                .thenThrow(new RuntimeException("unexpected"));

        ResponseEntity<Void> resp = controller.callback("github", "code", "state");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(resp.getHeaders().getLocation().toString()).contains("oauthError=true");
    }

    @Test
    void callback_linkFlow_success_redirectsToSettings() {
        when(oAuthService.peekLinkedUserId("link-state")).thenReturn("u1");
        doNothing().when(oAuthService).handleLinkCallback("github", "code123", "link-state");

        ResponseEntity<Void> resp = controller.callback("github", "code123", "link-state");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(resp.getHeaders().getLocation().toString()).contains("/settings?linked=true");
    }

    @Test
    void callback_linkFlow_conflict_redirectsToSettingsWithError() {
        when(oAuthService.peekLinkedUserId("link-state")).thenReturn("u1");
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "already linked"))
                .when(oAuthService)
                .handleLinkCallback("github", "code123", "link-state");

        ResponseEntity<Void> resp = controller.callback("github", "code123", "link-state");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(resp.getHeaders().getLocation().toString()).contains("/settings?linkError=true");
    }
}
