package com.magentamause.cosydomainprovider.configuration.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "auth.cookie")
public class AuthCookieProperties {
    private boolean secure;

    /** Path the refresh cookie is scoped to; must match the refresh token endpoint. */
    private String refreshPath = "/api/v1/auth/token";
}
