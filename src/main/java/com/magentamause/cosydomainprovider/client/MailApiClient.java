package com.magentamause.cosydomainprovider.client;

import com.magentamause.cosydomainprovider.client.mail.model.MailEntityResponse;
import com.magentamause.cosydomainprovider.client.mail.model.SendMailDto;
import com.magentamause.cosydomainprovider.configuration.client.MailApiClientProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@EnableConfigurationProperties(MailApiClientProperties.class)
public class MailApiClient {
    private final MailApiClientProperties mailApiClientProperties;
    private final WebClient webClient;

    public MailApiClient(
            MailApiClientProperties mailApiClientProperties, WebClient.Builder webClientBuilder) {
        webClientBuilder.baseUrl(mailApiClientProperties.url());
        this.mailApiClientProperties = mailApiClientProperties;
        this.webClient = webClientBuilder.build();
    }

    public Mono<MailEntityResponse> sendEmail(SendMailDto sendMailDto) {
        return webClient
                .post()
                .header("Authorization", mailApiClientProperties.apiKey())
                .bodyValue(sendMailDto)
                .retrieve()
                .bodyToMono(MailEntityResponse.class);
    }
}
