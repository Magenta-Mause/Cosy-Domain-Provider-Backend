package com.magentamause.cosydomainprovider.configuration.properties.aws;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "aws.route53")
public class Route53Properties {
    private String hostedZoneId;
    private String domain;
    private long defaultTtl = 60;
}
