package com.magentamause.cosydomainprovider.controller.v1.schema;

import com.magentamause.cosydomainprovider.model.action.LoginDto;
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

    @PostMapping("/verify/{uuid}")
    ResponseEntity<Void> verifyEmail(@PathVariable String uuid, @RequestParam String accessToken);
}
