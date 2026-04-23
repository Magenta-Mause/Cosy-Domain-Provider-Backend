package com.magentamause.cosydomainprovider.controller.v1.schema;

import com.magentamause.cosydomainprovider.model.action.EmailVerificationDto;
import com.magentamause.cosydomainprovider.model.action.ForgotPasswordDto;
import com.magentamause.cosydomainprovider.model.action.LoginDto;
import com.magentamause.cosydomainprovider.model.action.ResetPasswordDto;
import com.magentamause.cosydomainprovider.model.action.SetPasswordDto;
import com.magentamause.cosydomainprovider.model.action.TokenMode;
import com.magentamause.cosydomainprovider.model.action.UserCreationDto;
import com.magentamause.cosydomainprovider.model.core.LoginResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/v1/auth")
public interface AuthorizationApi {

    @PostMapping("/login")
    ResponseEntity<LoginResponseDto> login(
            @Valid @RequestBody LoginDto loginDto,
            @RequestParam(value = "tokenMode", defaultValue = "COOKIE") TokenMode tokenMode);

    @PostMapping("/register")
    ResponseEntity<LoginResponseDto> register(
            @Valid @RequestBody UserCreationDto userCreationDto,
            @RequestParam(value = "tokenMode", defaultValue = "COOKIE") TokenMode tokenMode);

    @GetMapping("/token")
    ResponseEntity<String> fetchToken(@CookieValue(value = "refreshToken") String refreshToken);

    @PostMapping("/logout")
    ResponseEntity<Void> logout();

    @PostMapping("/verify")
    ResponseEntity<Void> verifyEmail(@RequestBody EmailVerificationDto accessToken);

    @PostMapping("/resend-verification")
    ResponseEntity<Void> resendVerification();

    @PostMapping("/forgot-password")
    ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordDto forgotPasswordDto);

    @PostMapping("/reset-password")
    ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordDto resetPasswordDto);

    @PostMapping("/set-password")
    ResponseEntity<Void> setPassword(@Valid @RequestBody SetPasswordDto dto);
}
