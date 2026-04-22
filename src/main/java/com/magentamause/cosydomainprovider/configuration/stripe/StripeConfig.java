package com.magentamause.cosydomainprovider.configuration.stripe;

import com.stripe.Stripe;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StripeProperties.class)
public class StripeConfig {

    @Bean
    CommandLineRunner initStripe(StripeProperties props) {
        return args -> Stripe.apiKey = props.getApiKey();
    }
}
