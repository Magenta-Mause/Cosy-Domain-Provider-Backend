package com.magentamause.cosydomainprovider.configuration.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rate-limit")
public record RateLimitProperties(Rule auth, Rule global) {
    public record Rule(int requests, int periodSeconds) {}
}
