package com.magentamause.cosydomainprovider.security.jwtfilter;

import com.magentamause.cosydomainprovider.configuration.security.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import java.security.Key;
import java.util.Date;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtUtils {

    private static final String ISSUER = "cosy-domain-provider";

    private final JwtParser jwtParser;
    private final JwtProperties jwtProperties;
    private final Key jwtSigningKey;

    public long getTokenValidityDuration(JwtTokenBody.TokenType tokenType) {
        return tokenType == JwtTokenBody.TokenType.IDENTITY_TOKEN
                ? jwtProperties.getIdentityTokenExpirationTime()
                : jwtProperties.getRefreshTokenExpirationTime();
    }

    public String generateToken(JwtTokenBody body) {
        return createToken(body.toClaimsMap(), body.getUserId(), getTokenValidityDuration(body.getTokenType()));
    }

    private String createToken(Map<String, Object> claims, String subject, long expirationTime) {
        return Jwts.builder()
                .issuer(ISSUER)
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(jwtSigningKey)
                .compact();
    }

    public Claims getTokenContentBody(String token, JwtTokenBody.TokenType tokenType)
            throws SecurityException {
        try {
            Claims claims = jwtParser.parseSignedClaims(token).getPayload();

            if (!tokenType.toString().equals(claims.get("tokenType"))) {
                throw new SecurityException("Token type mismatch. Expected: " + tokenType);
            }
            if (!ISSUER.equals(claims.getIssuer())) {
                throw new SecurityException("Invalid token issuer");
            }
            if (claims.getSubject() == null || claims.getSubject().isEmpty()) {
                throw new SecurityException("Missing subject claim");
            }
            return claims;
        } catch (ExpiredJwtException e) {
            log.debug("Token expired: {}", e.getMessage());
            throw new SecurityException("Token expired");
        } catch (UnsupportedJwtException | MalformedJwtException e) {
            log.debug("Invalid token format: {}", e.getMessage());
            throw new SecurityException("Invalid token format");
        } catch (SecurityException e) {
            log.debug("Token validation failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.debug("Unexpected error while parsing JWT token", e);
            throw new SecurityException("Token validation failed");
        }
    }
}
