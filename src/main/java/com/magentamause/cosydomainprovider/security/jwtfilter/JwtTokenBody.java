package com.magentamause.cosydomainprovider.security.jwtfilter;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JwtTokenBody {
    private String username;
    private TokenType tokenType;

    public enum TokenType {
        REFRESH_TOKEN,
        IDENTITY_TOKEN,
    }
}
