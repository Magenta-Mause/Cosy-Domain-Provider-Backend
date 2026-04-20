package com.magentamause.cosydomainprovider.configuration.properties.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secretKey;
    private long identityTokenExpirationTime;
    private long refreshTokenExpirationTime;
}
