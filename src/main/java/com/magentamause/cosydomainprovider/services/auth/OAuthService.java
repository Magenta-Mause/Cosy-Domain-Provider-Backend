package com.magentamause.cosydomainprovider.services.auth;

import com.magentamause.cosydomainprovider.configuration.oauth.OAuthProperties;
import com.magentamause.cosydomainprovider.entity.OAuthIdentityEntity;
import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.repository.OAuthIdentityRepository;
import com.magentamause.cosydomainprovider.repository.UserRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {

    private static final long STATE_TTL_MS = 10 * 60 * 1000L;

    private final OAuthProperties oAuthProperties;
    private final OAuthIdentityRepository oAuthIdentityRepository;
    private final UserRepository userRepository;
    private final WebClient.Builder webClientBuilder;

    private final Map<String, Long> pendingStates = new ConcurrentHashMap<>();

    public String buildAuthorizationUrl(String provider) {
        OAuthProperties.ProviderConfig config = requireProvider(provider);
        String state = UUID.randomUUID().toString();
        pendingStates.put(state, System.currentTimeMillis());

        return config.getAuthorizationUri()
                + "?client_id="
                + encode(config.getClientId())
                + "&redirect_uri="
                + encode(config.getCallbackUri())
                + "&response_type=code"
                + "&scope="
                + encode(config.getScope())
                + "&state="
                + encode(state);
    }

    public UserEntity handleCallback(String provider, String code, String state) {
        validateState(state);
        OAuthProperties.ProviderConfig config = requireProvider(provider);

        String accessToken = exchangeCodeForToken(provider, config, code);
        OAuthUserInfo userInfo = fetchUserInfo(provider, config, accessToken);

        return resolveUser(provider, userInfo);
    }

    private void validateState(String state) {
        Long issuedAt = pendingStates.remove(state);
        if (issuedAt == null || System.currentTimeMillis() - issuedAt > STATE_TTL_MS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired OAuth state");
        }
    }

    private String exchangeCodeForToken(
            String provider, OAuthProperties.ProviderConfig config, String code) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", config.getCallbackUri());
        body.add("client_id", config.getClientId());
        body.add("client_secret", config.getClientSecret());

        Map<?, ?> response =
                webClientBuilder
                        .build()
                        .post()
                        .uri(config.getTokenUri())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .accept(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromFormData(body))
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

        if (response == null || !response.containsKey("access_token")) {
            log.error("Token exchange failed for provider {}: {}", provider, response);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Token exchange failed");
        }

        return (String) response.get("access_token");
    }

    @SuppressWarnings("unchecked")
    private OAuthUserInfo fetchUserInfo(
            String provider, OAuthProperties.ProviderConfig config, String accessToken) {
        Map<String, Object> userInfo =
                (Map<String, Object>)
                        webClientBuilder
                                .build()
                                .get()
                                .uri(config.getUserInfoUri())
                                .header("Authorization", "Bearer " + accessToken)
                                .accept(MediaType.APPLICATION_JSON)
                                .retrieve()
                                .bodyToMono(Map.class)
                                .block();

        if (userInfo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to fetch user info");
        }

        return switch (provider) {
            case "google" -> new OAuthUserInfo(
                    String.valueOf(userInfo.get("sub")),
                    String.valueOf(userInfo.get("email")),
                    String.valueOf(userInfo.getOrDefault("name", userInfo.get("email"))));
            case "github" -> {
                String email = userInfo.get("email") != null ? String.valueOf(userInfo.get("email")) : fetchGithubEmail(accessToken);
                yield new OAuthUserInfo(
                        String.valueOf(userInfo.get("id")),
                        email,
                        String.valueOf(userInfo.get("login")));
            }
            case "discord" -> new OAuthUserInfo(
                    String.valueOf(userInfo.get("id")),
                    String.valueOf(userInfo.get("email")),
                    String.valueOf(userInfo.get("username")));
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown provider: " + provider);
        };
    }

    @SuppressWarnings("unchecked")
    private String fetchGithubEmail(String accessToken) {
        List<Map<String, Object>> emails =
                (List<Map<String, Object>>)
                        webClientBuilder
                                .build()
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
                .or(() -> emails.stream().findFirst())
                .map(e -> String.valueOf(e.get("email")))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No usable GitHub email"));
    }

    private UserEntity resolveUser(String provider, OAuthUserInfo userInfo) {
        Optional<OAuthIdentityEntity> existingIdentity =
                oAuthIdentityRepository.findByProviderAndProviderSubject(
                        provider, userInfo.subject());

        if (existingIdentity.isPresent()) {
            return existingIdentity.get().getUser();
        }

        UserEntity user =
                userRepository
                        .findByEmailIgnoreCase(userInfo.email())
                        .orElseGet(() -> createOAuthUser(userInfo));

        oAuthIdentityRepository.save(
                OAuthIdentityEntity.builder()
                        .user(user)
                        .provider(provider)
                        .providerSubject(userInfo.subject())
                        .email(userInfo.email())
                        .build());

        return user;
    }

    private UserEntity createOAuthUser(OAuthUserInfo userInfo) {
        String username = sanitizeUsername(userInfo.username());
        if (userRepository.findByUsernameIgnoreCase(username).isPresent()) {
            username = username + "_" + UUID.randomUUID().toString().substring(0, 6);
        }
        UserEntity user =
                UserEntity.builder()
                        .username(username)
                        .email(userInfo.email())
                        .passwordHash(null)
                        .needsPasswordSetup(true)
                        .isVerified(true)
                        .build();
        return userRepository.save(user);
    }

    private String sanitizeUsername(String raw) {
        String sanitized = raw.replaceAll("[^a-zA-Z0-9_-]", "").toLowerCase();
        return sanitized.isEmpty() ? "user" : sanitized;
    }

    private OAuthProperties.ProviderConfig requireProvider(String provider) {
        if (oAuthProperties.getProviders() == null
                || !oAuthProperties.getProviders().containsKey(provider)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown provider: " + provider);
        }
        return oAuthProperties.getProviders().get(provider);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    record OAuthUserInfo(String subject, String email, String username) {}
}
