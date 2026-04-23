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
    public OAuthUserInfo fetchUserInfo(
            String accessToken, WebClient webClient, String userInfoUri) {
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

        String subject = requiredStringClaim(raw, "sub");
        String email = requiredStringClaim(raw, "email");
        String name = optionalStringClaim(raw, "name");

        return new OAuthUserInfo(subject, email, name != null ? name : email);
    }

    private String requiredStringClaim(Map<String, Object> raw, String claimName) {
        String value = optionalStringClaim(raw, claimName);
        if (value == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Missing required user info field: " + claimName);
        }
        return value;
    }

    private String optionalStringClaim(Map<String, Object> raw, String claimName) {
        Object value = raw.get(claimName);
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }
}
