package com.magentamause.cosydomainprovider.security.jwtfilter;

import static org.assertj.core.api.Assertions.*;

import com.magentamause.cosydomainprovider.configuration.security.JwtProperties;
import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.model.core.Plan;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtUtilsTest {

    private static final String SECRET =
            "e8996bf24977ce5615015c717447911799207025d1998a01256b7b7717bac18e";

    private JwtUtils jwtUtils;
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        JwtParser parser = Jwts.parser().verifyWith(key).build();

        jwtProperties = new JwtProperties();
        jwtProperties.setSecretKey(SECRET);
        jwtProperties.setIdentityTokenExpirationTime(3_600_000L);
        jwtProperties.setRefreshTokenExpirationTime(2_678_400_000L);
        jwtProperties.setMfaChallengeTokenExpirationTime(300_000L);

        jwtUtils = new JwtUtils(parser, jwtProperties, key);
    }

    private UserEntity user() {
        return UserEntity.builder()
                .uuid("u1")
                .username("alice")
                .email("alice@example.com")
                .isVerified(true)
                .plan(Plan.FREE)
                .build();
    }

    @Test
    void generateAndParseRefreshToken_roundtrip() {
        String token = jwtUtils.generateToken(JwtTokenBody.forRefreshToken(user()));
        Claims claims = jwtUtils.getTokenContentBody(token, JwtTokenBody.TokenType.REFRESH_TOKEN);
        assertThat(claims.getSubject()).isEqualTo("u1");
    }

    @Test
    void generateAndParseIdentityToken_roundtrip() {
        String token = jwtUtils.generateToken(JwtTokenBody.forIdentityToken(user(), 3));
        Claims claims = jwtUtils.getTokenContentBody(token, JwtTokenBody.TokenType.IDENTITY_TOKEN);
        assertThat(claims.getSubject()).isEqualTo("u1");
        assertThat(claims.get("email")).isEqualTo("alice@example.com");
    }

    @Test
    void generateAndParseMfaChallengeToken_roundtrip() {
        String token = jwtUtils.generateToken(JwtTokenBody.forMfaChallengeToken(user()));
        Claims claims =
                jwtUtils.getTokenContentBody(token, JwtTokenBody.TokenType.MFA_CHALLENGE_TOKEN);
        assertThat(claims.getSubject()).isEqualTo("u1");
    }

    @Test
    void getTokenContentBody_wrongType_throwsSecurityException() {
        String token = jwtUtils.generateToken(JwtTokenBody.forRefreshToken(user()));
        assertThatThrownBy(
                        () ->
                                jwtUtils.getTokenContentBody(
                                        token, JwtTokenBody.TokenType.IDENTITY_TOKEN))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void getTokenContentBody_malformedToken_throwsSecurityException() {
        assertThatThrownBy(
                        () ->
                                jwtUtils.getTokenContentBody(
                                        "not-a-jwt", JwtTokenBody.TokenType.REFRESH_TOKEN))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void getTokenValidityDuration_returnsCorrectValues() {
        assertThat(jwtUtils.getTokenValidityDuration(JwtTokenBody.TokenType.IDENTITY_TOKEN))
                .isEqualTo(3_600_000L);
        assertThat(jwtUtils.getTokenValidityDuration(JwtTokenBody.TokenType.REFRESH_TOKEN))
                .isEqualTo(2_678_400_000L);
        assertThat(jwtUtils.getTokenValidityDuration(JwtTokenBody.TokenType.MFA_CHALLENGE_TOKEN))
                .isEqualTo(300_000L);
    }
}
