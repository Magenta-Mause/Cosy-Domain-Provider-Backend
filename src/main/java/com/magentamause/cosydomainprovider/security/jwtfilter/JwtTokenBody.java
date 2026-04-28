package com.magentamause.cosydomainprovider.security.jwtfilter;

import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.model.core.Plan;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JwtTokenBody {
    private String username;
    private String userId;
    private String email;
    private boolean isVerified;
    private boolean needsPasswordSetup;
    private boolean isMfaEnabled;
    private Plan tier;
    private int maxSubdomainCount;
    private TokenType tokenType;

    public enum TokenType {
        REFRESH_TOKEN,
        IDENTITY_TOKEN,
        MFA_CHALLENGE_TOKEN,
    }

    public static JwtTokenBody forRefreshToken(UserEntity user) {
        return JwtTokenBody.builder()
                .tokenType(TokenType.REFRESH_TOKEN)
                .userId(user.getUuid())
                .username(user.getUsername())
                .build();
    }

    public static JwtTokenBody forIdentityToken(UserEntity user, int maxSubdomainCount) {
        return JwtTokenBody.builder()
                .tokenType(TokenType.IDENTITY_TOKEN)
                .userId(user.getUuid())
                .username(user.getUsername())
                .email(user.getEmail())
                .isVerified(user.isVerified())
                .needsPasswordSetup(user.isNeedsPasswordSetup())
                .isMfaEnabled(user.isMfaEnabled())
                .tier(user.getPlan())
                .maxSubdomainCount(maxSubdomainCount)
                .build();
    }

    public static JwtTokenBody forMfaChallengeToken(UserEntity user) {
        return JwtTokenBody.builder()
                .tokenType(TokenType.MFA_CHALLENGE_TOKEN)
                .userId(user.getUuid())
                .build();
    }

    public Map<String, Object> toClaimsMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("tokenType", tokenType);
        if (username != null) map.put("username", username);
        if (userId != null) map.put("userId", userId);
        if (email != null) map.put("email", email);
        if (tokenType == TokenType.IDENTITY_TOKEN) {
            map.put("isVerified", isVerified);
            map.put("needsPasswordSetup", needsPasswordSetup);
            map.put("isMfaEnabled", isMfaEnabled);
            if (tier != null) map.put("tier", tier.name());
            map.put("maxSubdomainCount", maxSubdomainCount);
        }
        return map;
    }
}
