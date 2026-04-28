package com.magentamause.cosydomainprovider.configuration.stagingauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "staging.auth")
public record StagingAuthProperties(boolean enabled, String username, String password) {}
