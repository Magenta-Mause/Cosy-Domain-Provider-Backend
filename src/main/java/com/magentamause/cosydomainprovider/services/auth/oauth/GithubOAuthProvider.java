package com.magentamause.cosydomainprovider.services.auth.oauth;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

@Component
class GithubOAuthProvider implements OAuthProviderClient {

    @Override
    public String providerName() {
        return "github";
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

        String email = raw.get("email") != null
                ? String.valueOf(raw.get("email"))
                : fetchPrimaryEmail(accessToken, webClient);

        return new OAuthUserInfo(String.valueOf(raw.get("id")), email, String.valueOf(raw.get("login")));
    }

    @SuppressWarnings("unchecked")
    private String fetchPrimaryEmail(String accessToken, WebClient webClient) {
        List<Map<String, Object>> emails =
                (List<Map<String, Object>>)
                        webClient
                                .get()
                                .uri("https://api.github.com/user/emails")
                                .header("Authorization", "Bearer " + accessToken)
                                .accept(MediaType.APPLICATION_JSON)
                                .retrieve()
                                .bodyToMono(List.class)
                                .block();

        if (emails == null || emails.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not retrieve GitHub email");
        }

        return emails.stream()
                .filter(e -> Boolean.TRUE.equals(e.get("primary")) && Boolean.TRUE.equals(e.get("verified")))
                .findFirst()
                .or(() -> emails.stream().filter(e -> Boolean.TRUE.equals(e.get("verified"))).findFirst())
                .map(e -> String.valueOf(e.get("email")))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No verified GitHub email available"));
    }
}
