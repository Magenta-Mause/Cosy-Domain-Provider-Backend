package com.magentamause.cosydomainprovider.configuration.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "client.mail")
public record MailApiClientProperties(String url, String apiKey) {

}
