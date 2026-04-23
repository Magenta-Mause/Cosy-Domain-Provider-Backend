package com.magentamause.cosydomainprovider.configuration.stripe;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("stripe")
public class StripeProperties {
    private String apiKey;
    private String webhookSecret;
    private String productId;
    private String priceId;
}
