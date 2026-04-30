package com.magentamause.cosydomainprovider.configuration.stagingauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "staging.captcha-bypass")
public record StagingCaptchaBypassProperties(boolean enabled) {}
