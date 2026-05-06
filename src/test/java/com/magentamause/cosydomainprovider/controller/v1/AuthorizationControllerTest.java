package com.magentamause.cosydomainprovider.controller.v1;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.magentamause.cosydomainprovider.controller.v1.implementation.AuthorizationController;
import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.model.action.*;
import com.magentamause.cosydomainprovider.model.core.LoginResponseDto;
import com.magentamause.cosydomainprovider.model.core.MfaSetupResponseDto;
import com.magentamause.cosydomainprovider.security.jwtfilter.JwtTokenBody;
import com.magentamause.cosydomainprovider.security.jwtfilter.JwtUtils;
import com.magentamause.cosydomainprovider.services.auth.AuthorizationService;
import com.magentamause.cosydomainprovider.services.auth.CaptchaService;
import com.magentamause.cosydomainprovider.services.auth.SecurityContextService;
import com.magentamause.cosydomainprovider.services.core.MfaService;
import com.magentamause.cosydomainprovider.services.core.PasswordResetService;
import com.magentamause.cosydomainprovider.services.core.UserService;
import com.magentamause.cosydomainprovider.services.core.UserVerificationService;
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
class AuthorizationControllerTest {

    @Mock private AuthorizationService authorizationService;
    @Mock private CaptchaService captchaService;
    @Mock private UserService userService;
    @Mock private UserVerificationService userVerificationService;
    @Mock private PasswordResetService passwordResetService;
    @Mock private JwtUtils jwtUtils;
    @Mock private SecurityContextService securityContextService;
    @Mock private MfaService mfaService;

    private AuthorizationController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthorizationController(
                authorizationService, captchaService, userService,
                userVerificationService, passwordResetService, jwtUtils,
                securityContextService, mfaService);
        when(jwtUtils.getTokenValidityDuration(JwtTokenBody.TokenType.REFRESH_TOKEN)).thenReturn(2_678_400_000L);
    }

    private UserEntity user() {
        return UserEntity.builder().uuid("u1").username("alice").email("a@a.com").isVerified(true).build();
    }

    // ---- login ----

    @Test
    void login_mfaRequired_returnsChallenge() {
        LoginDto dto = new LoginDto();
        dto.setEmail("a@a.com"); dto.setPassword("pass"); dto.setCaptchaToken("tok");
        LoginResponseDto loginResp = LoginResponseDto.builder().mfaRequired(true).challengeToken("challenge").build();
        when(authorizationService.loginUser("a@a.com", "pass")).thenReturn(loginResp);

        ResponseEntity<LoginResponseDto> resp = controller.login(dto, TokenMode.DIRECT);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getMfaRequired()).isTrue();
    }

    @Test
    void login_directMode_returnsRefreshToken() {
        LoginDto dto = new LoginDto();
        dto.setEmail("a@a.com"); dto.setPassword("pass"); dto.setCaptchaToken("tok");
        LoginResponseDto loginResp = LoginResponseDto.builder().refreshToken("rt").build();
        when(authorizationService.loginUser("a@a.com", "pass")).thenReturn(loginResp);

        ResponseEntity<LoginResponseDto> resp = controller.login(dto, TokenMode.DIRECT);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getRefreshToken()).isEqualTo("rt");
    }

    @Test
    void login_cookieMode_setsCookie() {
        LoginDto dto = new LoginDto();
        dto.setEmail("a@a.com"); dto.setPassword("pass"); dto.setCaptchaToken("tok");
        LoginResponseDto loginResp = LoginResponseDto.builder().refreshToken("rt").build();
        when(authorizationService.loginUser("a@a.com", "pass")).thenReturn(loginResp);

        ResponseEntity<LoginResponseDto> resp = controller.login(dto, TokenMode.COOKIE);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getHeaders().get(org.springframework.http.HttpHeaders.SET_COOKIE) != null).isTrue();
    }

    // ---- register ----

    @Test
    void register_directMode_returnsCreated() {
        UserCreationDto dto = UserCreationDto.builder().username("alice").email("a@a.com").password("password1").build();
        UserEntity created = user();
        when(userService.createUser(dto)).thenReturn(created);
        when(authorizationService.generateRefreshToken("u1")).thenReturn("rt");

        ResponseEntity<LoginResponseDto> resp = controller.register(dto, TokenMode.DIRECT);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(userVerificationService).sendInitialVerification(created);
    }

    @Test
    void register_cookieMode_setsCookie() {
        UserCreationDto dto = UserCreationDto.builder().username("alice").email("a@a.com").password("password1").build();
        UserEntity created = user();
        when(userService.createUser(dto)).thenReturn(created);
        when(authorizationService.generateRefreshToken("u1")).thenReturn("rt");

        ResponseEntity<LoginResponseDto> resp = controller.register(dto, TokenMode.COOKIE);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getHeaders().get(org.springframework.http.HttpHeaders.SET_COOKIE) != null).isTrue();
    }

    // ---- fetchToken ----

    @Test
    void fetchToken_returnsIdentityToken() {
        when(authorizationService.fetchIdentityTokenFromRefreshToken("rt")).thenReturn("it");
        ResponseEntity<String> resp = controller.fetchToken("rt");
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isEqualTo("it");
    }

    // ---- logout ----

    @Test
    void logout_returnsNoContentWithDeleteCookie() {
        ResponseEntity<Void> resp = controller.logout();
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(resp.getHeaders().get(org.springframework.http.HttpHeaders.SET_COOKIE) != null).isTrue();
    }

    // ---- resendVerification ----

    @Test
    void resendVerification_noUser_throwsUnauthorized() {
        when(securityContextService.getUser()).thenReturn(null);
        assertThatThrownBy(() -> controller.resendVerification())
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void resendVerification_success() {
        UserEntity u = user();
        when(securityContextService.getUser()).thenReturn(u);
        ResponseEntity<Void> resp = controller.resendVerification();
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(userVerificationService).resendVerificationCode("u1");
    }

    // ---- verifyEmail ----

    @Test
    void verifyEmail_authenticatedUser_verifiesById() {
        UserEntity u = user();
        when(securityContextService.getUser()).thenReturn(u);
        EmailVerificationDto dto = new EmailVerificationDto();
        dto.setToken("ABC123");

        controller.verifyEmail(dto);
        verify(userVerificationService).verifyUser("u1", "ABC123");
    }

    @Test
    void verifyEmail_unauthenticated_verifiesByToken() {
        when(securityContextService.getUser()).thenReturn(null);
        EmailVerificationDto dto = new EmailVerificationDto();
        dto.setToken("ABC123");

        controller.verifyEmail(dto);
        verify(userVerificationService).verifyUserByToken("ABC123");
    }

    // ---- forgotPassword ----

    @Test
    void forgotPassword_returnsNoContent() {
        ForgotPasswordDto dto = new ForgotPasswordDto();
        dto.setEmail("a@a.com");
        ResponseEntity<Void> resp = controller.forgotPassword(dto);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(passwordResetService).initiatePasswordReset("a@a.com");
    }

    // ---- resetPassword ----

    @Test
    void resetPassword_returnsNoContent() {
        ResetPasswordDto dto = new ResetPasswordDto();
        dto.setToken("tok"); dto.setNewPassword("newpass1");
        ResponseEntity<Void> resp = controller.resetPassword(dto);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(passwordResetService).confirmPasswordReset("tok", "newpass1");
    }

    // ---- setPassword ----

    @Test
    void setPassword_noUser_throwsUnauthorized() {
        when(securityContextService.getUser()).thenReturn(null);
        SetPasswordDto dto = new SetPasswordDto(); dto.setPassword("password1");
        assertThatThrownBy(() -> controller.setPassword(dto))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void setPassword_success() {
        UserEntity u = user();
        when(securityContextService.getUser()).thenReturn(u);
        SetPasswordDto dto = new SetPasswordDto(); dto.setPassword("password1");
        ResponseEntity<Void> resp = controller.setPassword(dto);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(userService).setPassword("u1", "password1");
    }

    // ---- setupMfa ----

    @Test
    void setupMfa_noUser_throwsUnauthorized() {
        when(securityContextService.getUser()).thenReturn(null);
        assertThatThrownBy(() -> controller.setupMfa())
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void setupMfa_success() {
        UserEntity u = user();
        when(securityContextService.getUser()).thenReturn(u);
        MfaSetupResponseDto dto = MfaSetupResponseDto.builder().totpUri("otpauth://...").secret("sec").build();
        when(mfaService.setupMfa(u)).thenReturn(dto);
        ResponseEntity<MfaSetupResponseDto> resp = controller.setupMfa();
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getSecret()).isEqualTo("sec");
    }

    // ---- confirmMfa ----

    @Test
    void confirmMfa_noUser_throwsUnauthorized() {
        when(securityContextService.getUser()).thenReturn(null);
        MfaConfirmDto dto = new MfaConfirmDto(); dto.setTotpCode("123456");
        assertThatThrownBy(() -> controller.confirmMfa(dto))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void confirmMfa_success() {
        UserEntity u = user();
        when(securityContextService.getUser()).thenReturn(u);
        MfaConfirmDto dto = new MfaConfirmDto(); dto.setTotpCode("123456");
        ResponseEntity<Void> resp = controller.confirmMfa(dto);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(mfaService).confirmMfa(u, "123456");
    }

    // ---- completeMfaChallenge ----

    @Test
    void completeMfaChallenge_directMode_returnsToken() {
        MfaChallengeDto dto = new MfaChallengeDto();
        dto.setChallengeToken("ct"); dto.setTotpCode("123456");
        when(authorizationService.completeMfaChallenge("ct", "123456")).thenReturn("rt");

        ResponseEntity<LoginResponseDto> resp = controller.completeMfaChallenge(dto, TokenMode.DIRECT);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getRefreshToken()).isEqualTo("rt");
    }
}
