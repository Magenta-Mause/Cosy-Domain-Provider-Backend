package com.magentamause.cosydomainprovider.services.auth;

import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.security.jwtfilter.JwtTokenBody;
import com.magentamause.cosydomainprovider.security.jwtfilter.JwtUtils;
import com.magentamause.cosydomainprovider.services.core.UserService;
import io.jsonwebtoken.Claims;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private final JwtUtils jwtUtils;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public String loginUser(String email, String plainPassword) {
        UserEntity user;
        try {
            user = userService.getUserByEmail(email);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        if (!passwordEncoder.matches(plainPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return generateRefreshToken(user.getUuid());
    }

    public String fetchIdentityTokenFromRefreshToken(String refreshToken) {
        Claims claims;
        try {
            claims =
                    jwtUtils.getTokenContentBody(
                            refreshToken, JwtTokenBody.TokenType.REFRESH_TOKEN);
        } catch (SecurityException e) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
        }
        return generateIdentityToken(claims.getSubject());
    }

    public String generateIdentityToken(String userId) {
        return jwtUtils.generateIdentityToken(buildClaims(userId), userId);
    }

    public String generateRefreshToken(String userId) {
        return jwtUtils.generateRefreshToken(buildClaims(userId), userId);
    }

    private Map<String, Object> buildClaims(String userId) {
        UserEntity user = userService.getUserByUuid(userId);
        return Map.of("username", user.getUsername());
    }
}
