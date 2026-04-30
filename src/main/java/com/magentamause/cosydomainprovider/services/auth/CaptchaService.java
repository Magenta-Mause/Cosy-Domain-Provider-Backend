package com.magentamause.cosydomainprovider.services.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.magentamause.cosydomainprovider.configuration.stagingauth.StagingCaptchaBypassProperties;
import com.magentamause.cosydomainprovider.configuration.turnstile.TurnstileProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CaptchaService {

    private final WebClient webClient;
    private final TurnstileProperties turnstileProperties;
    private final StagingCaptchaBypassProperties bypassProperties;

    public CaptchaService(
            @Qualifier("turnstileWebClient") WebClient webClient,
            TurnstileProperties turnstileProperties,
            StagingCaptchaBypassProperties bypassProperties) {
        this.webClient = webClient;
        this.turnstileProperties = turnstileProperties;
        this.bypassProperties = bypassProperties;
    }

    public void verifyCaptcha(String token) {
        if (bypassProperties.enabled() && hasBypassSignal()) {
            return;
        }

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

    private boolean hasBypassSignal() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return false;
        }
        HttpServletRequest request = attrs.getRequest();

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("CAPTCHA_BYPASS".equals(c.getName()) && "1".equals(c.getValue())) {
                    return true;
                }
            }
        }

        return "1".equals(request.getParameter("captchaBypass"));
    }

    private record TurnstileResponse(@JsonProperty("success") boolean success) {}
}
