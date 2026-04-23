package com.magentamause.cosydomainprovider.configuration.oauth;

import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "oauth")
public class OAuthProperties {
    private String frontendUrl = "http://localhost:5173";
    private boolean secureCookie = false;
    private Map<String, ProviderConfig> providers;

    @Data
    public static class ProviderConfig {
        private String clientId;
        private String clientSecret;
        private String authorizationUri;
        private String tokenUri;
        private String userInfoUri;
        private String scope;
        private String callbackUri;
    }
}
