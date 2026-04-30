package com.magentamause.cosydomainprovider.controller.v1.implementation;

import com.magentamause.cosydomainprovider.controller.v1.schema.AuthorizationApi;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
public class AuthorizationController implements AuthorizationApi {

    private static final int MILLISECONDS_IN_SECOND = 1000;
    private static final String REFRESH_COOKIE_PATH = "/api/v1/auth/token";

    private final AuthorizationService authorizationService;
    private final CaptchaService captchaService;
    private final UserService userService;
    private final UserVerificationService userVerificationService;
    private final PasswordResetService passwordResetService;
    private final JwtUtils jwtUtils;
    private final SecurityContextService securityContextService;
    private final MfaService mfaService;

    @Override
    public ResponseEntity<LoginResponseDto> login(LoginDto loginDto, TokenMode tokenMode) {
        captchaService.verifyCaptcha(loginDto.getCaptchaToken());
        LoginResponseDto loginResult =
                authorizationService.loginUser(loginDto.getEmail(), loginDto.getPassword());
        if (Boolean.TRUE.equals(loginResult.getMfaRequired())) {
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .header("Pragma", "no-cache")
                    .body(loginResult);
        }
        return buildRefreshTokenResponse(loginResult.getRefreshToken(), tokenMode, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<LoginResponseDto> register(
            UserCreationDto userCreationDto, TokenMode tokenMode) {
        captchaService.verifyCaptcha(userCreationDto.getCaptchaToken());
        UserEntity user = userService.createUser(userCreationDto);
        userVerificationService.sendInitialVerification(user);
        String refreshToken = authorizationService.generateRefreshToken(user.getUuid());
        return buildRefreshTokenResponse(refreshToken, tokenMode, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<String> fetchToken(String refreshToken) {
        return ResponseEntity.ok(
                authorizationService.fetchIdentityTokenFromRefreshToken(refreshToken));
    }

    @Override
    public ResponseEntity<Void> logout() {
        ResponseCookie deleteCookie =
                ResponseCookie.from("refreshToken", "")
                        .httpOnly(true)
                        .secure(false)
                        .path(REFRESH_COOKIE_PATH)
                        .maxAge(0)
                        .sameSite("Strict")
                        .build();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .build();
    }

    @Override
    public ResponseEntity<Void> resendVerification() {
        UserEntity user = securityContextService.getUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        userVerificationService.resendVerificationCode(user.getUuid());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> verifyEmail(EmailVerificationDto accessToken) {
        UserEntity user = securityContextService.getUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        userVerificationService.verifyUser(user.getUuid(), accessToken.getToken());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> forgotPassword(ForgotPasswordDto forgotPasswordDto) {
        passwordResetService.initiatePasswordReset(forgotPasswordDto.getEmail());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> resetPassword(ResetPasswordDto resetPasswordDto) {
        passwordResetService.confirmPasswordReset(
                resetPasswordDto.getToken(), resetPasswordDto.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> setPassword(SetPasswordDto dto) {
        UserEntity user = securityContextService.getUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        userService.setPassword(user.getUuid(), dto.getPassword());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<MfaSetupResponseDto> setupMfa() {
        UserEntity user = securityContextService.getUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        return ResponseEntity.ok(mfaService.setupMfa(user));
    }

    @Override
    public ResponseEntity<Void> confirmMfa(MfaConfirmDto dto) {
        UserEntity user = securityContextService.getUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        mfaService.confirmMfa(user, dto.getTotpCode());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<LoginResponseDto> completeMfaChallenge(
            MfaChallengeDto dto, TokenMode tokenMode) {
        String refreshToken =
                authorizationService.completeMfaChallenge(
                        dto.getChallengeToken(), dto.getTotpCode());
        return buildRefreshTokenResponse(refreshToken, tokenMode, HttpStatus.OK);
    }

    private ResponseEntity<LoginResponseDto> buildRefreshTokenResponse(
            String refreshToken, TokenMode tokenMode, HttpStatus successStatus) {
        if (tokenMode == TokenMode.DIRECT) {
            return ResponseEntity.status(successStatus)
                    .cacheControl(CacheControl.noStore())
                    .header("Pragma", "no-cache")
                    .body(LoginResponseDto.builder().refreshToken(refreshToken).build());
        }

        ResponseCookie responseCookie =
                ResponseCookie.from("refreshToken", refreshToken)
                        .httpOnly(true)
                        .secure(false)
                        .maxAge(
                                jwtUtils.getTokenValidityDuration(
                                                JwtTokenBody.TokenType.REFRESH_TOKEN)
                                        / MILLISECONDS_IN_SECOND)
                        .path(REFRESH_COOKIE_PATH)
                        .sameSite("Strict")
                        .build();
        return ResponseEntity.status(successStatus)
                .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                .build();
    }
}
