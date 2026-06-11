package com.magentamause.cosydomainprovider.configuration.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "auth.cookie")
public class AuthCookieProperties {
    private boolean secure;
}
