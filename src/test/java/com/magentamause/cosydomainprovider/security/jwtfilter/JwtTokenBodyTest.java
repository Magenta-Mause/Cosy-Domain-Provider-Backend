package com.magentamause.cosydomainprovider.security.jwtfilter;

import static org.assertj.core.api.Assertions.*;

import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.model.core.Plan;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JwtTokenBodyTest {

    private UserEntity user() {
        return UserEntity.builder()
                .uuid("u1")
                .username("alice")
                .email("alice@example.com")
                .isVerified(true)
                .needsPasswordSetup(false)
                .isMfaEnabled(false)
                .plan(Plan.FREE)
                .build();
    }

    @Test
    void forRefreshToken_hasCorrectFields() {
        JwtTokenBody body = JwtTokenBody.forRefreshToken(user());
        assertThat(body.getTokenType()).isEqualTo(JwtTokenBody.TokenType.REFRESH_TOKEN);
        assertThat(body.getUserId()).isEqualTo("u1");
        assertThat(body.getUsername()).isEqualTo("alice");
    }

    @Test
    void forIdentityToken_hasCorrectFields() {
        JwtTokenBody body = JwtTokenBody.forIdentityToken(user(), 3);
        assertThat(body.getTokenType()).isEqualTo(JwtTokenBody.TokenType.IDENTITY_TOKEN);
        assertThat(body.getEmail()).isEqualTo("alice@example.com");
        assertThat(body.getMaxSubdomainCount()).isEqualTo(3);
        assertThat(body.getTier()).isEqualTo(Plan.FREE);
    }

    @Test
    void forMfaChallengeToken_hasCorrectFields() {
        JwtTokenBody body = JwtTokenBody.forMfaChallengeToken(user());
        assertThat(body.getTokenType()).isEqualTo(JwtTokenBody.TokenType.MFA_CHALLENGE_TOKEN);
        assertThat(body.getUserId()).isEqualTo("u1");
    }

    @Test
    void toClaimsMap_refreshToken_containsTokenType() {
        JwtTokenBody body = JwtTokenBody.forRefreshToken(user());
        Map<String, Object> claims = body.toClaimsMap();
        assertThat(claims).containsKey("tokenType");
        assertThat(claims.get("tokenType").toString()).isEqualTo("REFRESH_TOKEN");
    }

    @Test
    void toClaimsMap_identityToken_containsAllFields() {
        JwtTokenBody body = JwtTokenBody.forIdentityToken(user(), 2);
        Map<String, Object> claims = body.toClaimsMap();
        assertThat(claims)
                .containsKeys("tokenType", "email", "isVerified", "tier", "maxSubdomainCount");
    }

    @Test
    void toClaimsMap_mfaChallenge_doesNotContainIdentityFields() {
        JwtTokenBody body = JwtTokenBody.forMfaChallengeToken(user());
        Map<String, Object> claims = body.toClaimsMap();
        assertThat(claims).doesNotContainKey("isVerified");
        assertThat(claims).doesNotContainKey("email");
    }
}
