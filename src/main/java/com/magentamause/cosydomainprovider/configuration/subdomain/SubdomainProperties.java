package com.magentamause.cosydomainprovider.configuration.subdomain;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "subdomain")
public class SubdomainProperties {
    private int maxPerUser = 5;
    private List<String> reservedLabels = List.of();
}
