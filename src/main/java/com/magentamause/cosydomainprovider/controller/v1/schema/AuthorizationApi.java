package com.magentamause.cosydomainprovider.controller.v1.schema;

import com.magentamause.cosydomainprovider.model.action.EmailVerificationDto;
import com.magentamause.cosydomainprovider.model.action.ForgotPasswordDto;
import com.magentamause.cosydomainprovider.model.action.LoginDto;
import com.magentamause.cosydomainprovider.model.action.ResetPasswordDto;
import com.magentamause.cosydomainprovider.model.action.SetPasswordDto;
import com.magentamause.cosydomainprovider.model.action.TokenMode;
import com.magentamause.cosydomainprovider.model.action.UserCreationDto;
import com.magentamause.cosydomainprovider.model.core.LoginResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authorization", description = "Authentication, registration, and account management")
@RequestMapping("/v1/auth")
public interface AuthorizationApi {

    @Operation(summary = "Login with email and password")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    ResponseEntity<LoginResponseDto> login(
            @Valid @RequestBody LoginDto loginDto,
            @Parameter(description = "COOKIE sets httpOnly cookie, DIRECT returns token in body")
                    @RequestParam(value = "tokenMode", defaultValue = "COOKIE")
                    TokenMode tokenMode);

    @Operation(summary = "Register a new user account")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Account created"),
        @ApiResponse(responseCode = "409", description = "Email already in use")
    })
    @PostMapping("/register")
    ResponseEntity<LoginResponseDto> register(
            @Valid @RequestBody UserCreationDto userCreationDto,
            @Parameter(description = "COOKIE sets httpOnly cookie, DIRECT returns token in body")
                    @RequestParam(value = "tokenMode", defaultValue = "COOKIE")
                    TokenMode tokenMode);

    @Operation(summary = "Exchange refresh token cookie for a short-lived identity token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Identity token returned"),
        @ApiResponse(responseCode = "400", description = "Missing or invalid refresh token cookie")
    })
    @GetMapping("/token")
    ResponseEntity<String> fetchToken(@CookieValue(value = "refreshToken") String refreshToken);

    @Operation(summary = "Logout — clears the refresh token cookie")
    @ApiResponse(responseCode = "204", description = "Logged out")
    @PostMapping("/logout")
    ResponseEntity<Void> logout();

    @Operation(summary = "Verify email address with the code sent on registration")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Email verified"),
        @ApiResponse(responseCode = "401", description = "Invalid or expired verification code")
    })
    @PostMapping("/verify")
    ResponseEntity<Void> verifyEmail(@RequestBody EmailVerificationDto accessToken);

    @Operation(summary = "Resend email verification code")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Verification email sent"),
        @ApiResponse(responseCode = "409", description = "Email already verified")
    })
    @PostMapping("/resend-verification")
    ResponseEntity<Void> resendVerification();

    @Operation(summary = "Initiate password reset — sends reset link to email")
    @ApiResponse(
            responseCode = "204",
            description = "Reset email sent (always, to avoid enumeration)")
    @PostMapping("/forgot-password")
    ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordDto forgotPasswordDto);

    @Operation(summary = "Complete password reset using token from email")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Password reset successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid or expired reset token")
    })
    @PostMapping("/reset-password")
    ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordDto resetPasswordDto);

    @Operation(
            summary = "Set initial password for OAuth-created accounts",
            description =
                    "Only available when needsPasswordSetup is true (OAuth accounts without a password).")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Password set"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "409", description = "Password already set")
    })
    @PostMapping("/set-password")
    ResponseEntity<Void> setPassword(@Valid @RequestBody SetPasswordDto dto);
}
