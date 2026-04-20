package com.magentamause.cosydomainprovider.configuration.properties.aws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.route53.Route53Client;

@Configuration
public class AwsConfig {

    @Value("${aws.region}")
    private String region;

    @Bean
    public Route53Client route53Client() {
        return Route53Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
