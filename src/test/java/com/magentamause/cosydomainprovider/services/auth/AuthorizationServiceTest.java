package com.magentamause.cosydomainprovider.services.auth;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.magentamause.cosydomainprovider.configuration.subdomain.SubdomainProperties;
import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.model.core.LoginResponseDto;
import com.magentamause.cosydomainprovider.model.core.Plan;
import com.magentamause.cosydomainprovider.model.exception.UserNotFoundException;
import com.magentamause.cosydomainprovider.security.jwtfilter.JwtTokenBody;
import com.magentamause.cosydomainprovider.security.jwtfilter.JwtUtils;
import com.magentamause.cosydomainprovider.services.core.MfaService;
import com.magentamause.cosydomainprovider.services.core.UserService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock private JwtUtils jwtUtils;
    @Mock private UserService userService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private SubdomainProperties subdomainProperties;
    @Mock private MfaService mfaService;

    private AuthorizationService service;

    @BeforeEach
    void setUp() {
        service = new AuthorizationService(jwtUtils, userService, passwordEncoder, subdomainProperties, mfaService);
    }

    private UserEntity user(boolean mfaEnabled) {
        return UserEntity.builder()
                .uuid("u1")
                .username("alice")
                .email("alice@example.com")
                .passwordHash("hashed")
                .isMfaEnabled(mfaEnabled)
                .plan(Plan.FREE)
                .build();
    }

    // ---- loginUser ----

    @Test
    void loginUser_userNotFound_throwsUnauthorized() {
        when(userService.getUserByEmail("x@x.com")).thenThrow(UserNotFoundException.byEmail("x@x.com"));
        assertThatThrownBy(() -> service.loginUser("x@x.com", "pass"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void loginUser_wrongPassword_throwsUnauthorized() {
        UserEntity u = user(false);
        when(userService.getUserByEmail("alice@example.com")).thenReturn(u);
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> service.loginUser("alice@example.com", "wrong"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void loginUser_nullPasswordHash_throwsUnauthorized() {
        UserEntity u = user(false);
        u.setPasswordHash(null);
        when(userService.getUserByEmail("alice@example.com")).thenReturn(u);

        assertThatThrownBy(() -> service.loginUser("alice@example.com", "pass"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void loginUser_noMfa_returnsRefreshToken() {
        UserEntity u = user(false);
        when(userService.getUserByEmail("alice@example.com")).thenReturn(u);
        when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);
        when(userService.getUserByUuid("u1")).thenReturn(u);
        when(jwtUtils.generateToken(any())).thenReturn("refresh-tok");

        LoginResponseDto result = service.loginUser("alice@example.com", "pass");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-tok");
        assertThat(result.getMfaRequired()).isNull();
    }

    @Test
    void loginUser_withMfa_returnsChallengeToken() {
        UserEntity u = user(true);
        when(userService.getUserByEmail("alice@example.com")).thenReturn(u);
        when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);
        when(jwtUtils.generateToken(any())).thenReturn("challenge-tok");

        LoginResponseDto result = service.loginUser("alice@example.com", "pass");
        assertThat(result.getMfaRequired()).isTrue();
        assertThat(result.getChallengeToken()).isEqualTo("challenge-tok");
    }

    // ---- completeMfaChallenge ----

    @Test
    void completeMfaChallenge_invalidToken_throws() {
        when(jwtUtils.getTokenContentBody("bad-tok", JwtTokenBody.TokenType.MFA_CHALLENGE_TOKEN))
                .thenThrow(new SecurityException("invalid"));
        assertThatThrownBy(() -> service.completeMfaChallenge("bad-tok", "123456"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void completeMfaChallenge_wrongTotpCode_throws() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("u1");
        when(jwtUtils.getTokenContentBody("tok", JwtTokenBody.TokenType.MFA_CHALLENGE_TOKEN)).thenReturn(claims);
        UserEntity u = user(true);
        u.setMfaSecret("secret");
        when(userService.getUserByUuid("u1")).thenReturn(u);
        when(mfaService.verifyCode("secret", "000000")).thenReturn(false);

        assertThatThrownBy(() -> service.completeMfaChallenge("tok", "000000"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void completeMfaChallenge_success_returnsRefreshToken() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("u1");
        when(jwtUtils.getTokenContentBody("tok", JwtTokenBody.TokenType.MFA_CHALLENGE_TOKEN)).thenReturn(claims);
        UserEntity u = user(true);
        u.setMfaSecret("secret");
        when(userService.getUserByUuid("u1")).thenReturn(u);
        when(mfaService.verifyCode("secret", "123456")).thenReturn(true);
        when(jwtUtils.generateToken(any())).thenReturn("refresh-tok");

        String result = service.completeMfaChallenge("tok", "123456");
        assertThat(result).isEqualTo("refresh-tok");
    }

    // ---- fetchIdentityTokenFromRefreshToken ----

    @Test
    void fetchIdentityToken_invalidRefreshToken_throws() {
        when(jwtUtils.getTokenContentBody("bad", JwtTokenBody.TokenType.REFRESH_TOKEN))
                .thenThrow(new SecurityException("invalid"));
        assertThatThrownBy(() -> service.fetchIdentityTokenFromRefreshToken("bad"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void fetchIdentityToken_success() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("u1");
        when(jwtUtils.getTokenContentBody("refresh", JwtTokenBody.TokenType.REFRESH_TOKEN)).thenReturn(claims);
        UserEntity u = user(false);
        when(userService.getUserByUuid("u1")).thenReturn(u);
        when(subdomainProperties.getMaxPerFreeUser()).thenReturn(1);
        when(subdomainProperties.getMaxPerPlusUser()).thenReturn(5);
        when(jwtUtils.generateToken(any())).thenReturn("identity-tok");

        String result = service.fetchIdentityTokenFromRefreshToken("refresh");
        assertThat(result).isEqualTo("identity-tok");
    }
}
