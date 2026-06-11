package com.magentamause.cosydomainprovider.configuration.web;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "frontend")
public class FrontendProperties {
    private String url;
}
