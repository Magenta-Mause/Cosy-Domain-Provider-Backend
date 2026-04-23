package com.magentamause.cosydomainprovider.services.auth.oauth;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

@Component
class DiscordOAuthProvider implements OAuthProviderClient {

    @Override
    public String providerName() {
        return "discord";
    }

    @Override
    @SuppressWarnings("unchecked")
    public OAuthUserInfo fetchUserInfo(String accessToken, WebClient webClient, String userInfoUri) {
        Map<String, Object> raw =
                (Map<String, Object>)
                        webClient
                                .get()
                                .uri(userInfoUri)
                                .header("Authorization", "Bearer " + accessToken)
                                .accept(MediaType.APPLICATION_JSON)
                                .retrieve()
                                .bodyToMono(Map.class)
                                .block();

        if (raw == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to fetch user info");
        }

        return new OAuthUserInfo(
                getRequiredString(raw, "id"),
                getRequiredString(raw, "email"),
                getRequiredString(raw, "username"));
    }

    private String getRequiredString(Map<String, Object> raw, String fieldName) {
        Object value = raw.get(fieldName);
        if (!(value instanceof String stringValue) || stringValue.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Missing required Discord user info field: " + fieldName);
        }
        return stringValue;
    }
}
