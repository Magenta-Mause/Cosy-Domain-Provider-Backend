package com.magentamause.cosydomainprovider.services.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.magentamause.cosydomainprovider.configuration.turnstile.TurnstileProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CaptchaService {

    private final WebClient webClient;
    private final TurnstileProperties turnstileProperties;

    public CaptchaService(
            @Qualifier("turnstileWebClient") WebClient webClient,
            TurnstileProperties turnstileProperties) {
        this.webClient = webClient;
        this.turnstileProperties = turnstileProperties;
    }

    public void verifyCaptcha(String token) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("secret", turnstileProperties.getSecretKey());
        formData.add("response", token);

        TurnstileResponse response =
                webClient
                        .post()
                        .uri("/turnstile/v0/siteverify")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(BodyInserters.fromFormData(formData))
                        .retrieve()
                        .bodyToMono(TurnstileResponse.class)
                        .block();

        if (response == null || !response.success()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Captcha verification failed");
        }
    }

    private record TurnstileResponse(@JsonProperty("success") boolean success) {}
}
