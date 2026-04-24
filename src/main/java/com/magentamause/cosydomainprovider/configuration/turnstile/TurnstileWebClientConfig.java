package com.magentamause.cosydomainprovider.configuration.turnstile;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class TurnstileWebClientConfig {

    @Bean("turnstileWebClient")
    public WebClient turnstileWebClient(WebClient.Builder builder) {
        return builder.baseUrl("https://challenges.cloudflare.com").build();
    }
}
