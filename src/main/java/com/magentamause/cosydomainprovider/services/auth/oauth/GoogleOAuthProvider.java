package com.magentamause.cosydomainprovider.services.auth.oauth;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

@Component
class GoogleOAuthProvider implements OAuthProviderClient {

    @Override
    public String providerName() {
        return "google";
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

        Object name = raw.get("name");
        return new OAuthUserInfo(
                String.valueOf(raw.get("sub")),
                String.valueOf(raw.get("email")),
                name != null ? String.valueOf(name) : String.valueOf(raw.get("email")));
    }
}
