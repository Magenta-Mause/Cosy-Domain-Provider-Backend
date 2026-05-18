package com.magentamause.cosydomainprovider.services.auth.oauth;

import com.magentamause.cosydomainprovider.configuration.oauth.OAuthProperties;
import com.magentamause.cosydomainprovider.entity.OAuthIdentityEntity;
import com.magentamause.cosydomainprovider.entity.OAuthStateEntity;
import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.model.core.OAuthIdentityDto;
import com.magentamause.cosydomainprovider.repository.OAuthIdentityRepository;
import com.magentamause.cosydomainprovider.repository.UserRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
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
public class OAuthService {

    private final OAuthProperties oAuthProperties;
    private final OAuthIdentityRepository oAuthIdentityRepository;
    private final UserRepository userRepository;
    private final OAuthStateStore stateStore;
    private final WebClient webClient;
    private final Map<String, OAuthProviderClient> providers;

    public OAuthService(
            OAuthProperties oAuthProperties,
            OAuthIdentityRepository oAuthIdentityRepository,
            UserRepository userRepository,
            OAuthStateStore stateStore,
            @Qualifier("oAuthWebClient") WebClient webClient,
            List<OAuthProviderClient> providerList) {
        this.oAuthProperties = oAuthProperties;
        this.oAuthIdentityRepository = oAuthIdentityRepository;
        this.userRepository = userRepository;
        this.stateStore = stateStore;
        this.webClient = webClient;
        this.providers =
                providerList.stream()
                        .collect(
                                Collectors.toMap(
                                        OAuthProviderClient::providerName, Function.identity()));
    }

    public String buildAuthorizationUrl(String provider) {
        OAuthProperties.ProviderConfig config = requireProvider(provider);
        String state = stateStore.generateState();
        return buildUrl(config, state);
    }

    public String buildLinkAuthorizationUrl(String provider, String userId) {
        OAuthProperties.ProviderConfig config = requireProvider(provider);
        String state = stateStore.generateLinkState(userId);
        return buildUrl(config, state);
    }

    private String buildUrl(OAuthProperties.ProviderConfig config, String state) {
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

    public UserEntity handleLoginCallback(String provider, String code, String state) {
        OAuthStateEntity consumed = consumeValidState(state);
        if (consumed.getLinkedUserId() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "State is for a link flow, not login");
        }
        OAuthProperties.ProviderConfig config = requireProvider(provider);
        String accessToken = exchangeCodeForToken(provider, config, code);
        OAuthUserInfo userInfo =
                providers
                        .get(provider)
                        .fetchUserInfo(accessToken, webClient, config.getUserInfoUri());
        validateUserInfo(provider, userInfo);
        return resolveLoginUser(provider, userInfo);
    }

    public void handleLinkCallback(String provider, String code, String state) {
        OAuthStateEntity consumed = consumeValidState(state);
        if (consumed.getLinkedUserId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "State is for a login flow, not link");
        }
        String userId = consumed.getLinkedUserId();
        OAuthProperties.ProviderConfig config = requireProvider(provider);
        String accessToken = exchangeCodeForToken(provider, config, code);
        OAuthUserInfo userInfo =
                providers
                        .get(provider)
                        .fetchUserInfo(accessToken, webClient, config.getUserInfoUri());
        validateUserInfo(provider, userInfo);

        if (oAuthIdentityRepository
                .findByProviderAndProviderSubject(provider, userInfo.subject())
                .isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This " + provider + " account is already linked to a user");
        }
        UserEntity user =
                userRepository
                        .findById(userId)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "User not found"));
        oAuthIdentityRepository.save(
                OAuthIdentityEntity.builder()
                        .user(user)
                        .provider(provider)
                        .providerSubject(userInfo.subject())
                        .email(userInfo.email())
                        .build());
        log.info("Linked {} identity to user {}", provider, userId);
    }

    public List<OAuthIdentityDto> getLinkedIdentities(String userId) {
        return oAuthIdentityRepository.findAllByUser_Uuid(userId).stream()
                .map(
                        e ->
                                OAuthIdentityDto.builder()
                                        .provider(e.getProvider())
                                        .email(e.getEmail())
                                        .build())
                .toList();
    }

    public void unlinkIdentity(String provider, String userId) {
        OAuthIdentityEntity identity =
                oAuthIdentityRepository
                        .findByProviderAndUser_Uuid(provider, userId)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "No "
                                                        + provider
                                                        + " identity linked to this account"));

        UserEntity user =
                userRepository
                        .findById(userId)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "User not found"));

        boolean hasPassword = user.getPasswordHash() != null;
        long linkedCount = oAuthIdentityRepository.countByUser_Uuid(userId);
        if (!hasPassword && linkedCount <= 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot unlink the only authentication method. Set a password first.");
        }

        oAuthIdentityRepository.delete(identity);
        log.info("Unlinked {} identity from user {}", provider, userId);
    }

    public String peekLinkedUserId(String state) {
        return stateStore.peekLinkedUserId(state);
    }

    private OAuthStateEntity consumeValidState(String state) {
        return stateStore
                .consumeState(state)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST, "Invalid or expired OAuth state"));
    }

    private void validateUserInfo(String provider, OAuthUserInfo userInfo) {
        if (userInfo == null
                || isMissingRequiredField(userInfo.subject())
                || isMissingRequiredField(userInfo.email())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "OAuth provider returned incomplete user information for " + provider);
        }
    }

    private boolean isMissingRequiredField(String value) {
        return value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value.trim());
    }

    private String exchangeCodeForToken(
            String provider, OAuthProperties.ProviderConfig config, String code) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", config.getCallbackUri());
        body.add("client_id", config.getClientId());
        body.add("client_secret", config.getClientSecret());

        @SuppressWarnings("unchecked")
        Map<String, Object> response =
                (Map<String, Object>)
                        webClient
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

        return String.valueOf(response.get("access_token"));
    }

    private UserEntity resolveLoginUser(String provider, OAuthUserInfo userInfo) {
        Optional<OAuthIdentityEntity> existingIdentity =
                oAuthIdentityRepository.findByProviderAndProviderSubject(
                        provider, userInfo.subject());

        if (existingIdentity.isPresent()) {
            return existingIdentity.get().getUser();
        }

        if (userRepository.findByEmailIgnoreCase(userInfo.email()).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An account with this email already exists. Please log in with your password and link "
                            + provider
                            + " from account settings.");
        }

        UserEntity user = createOAuthUser(userInfo);
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
        return userRepository.save(
                UserEntity.builder()
                        .username(username)
                        .email(userInfo.email())
                        .passwordHash(null)
                        .needsPasswordSetup(true)
                        .isVerified(false)
                        .build());
    }

    private String sanitizeUsername(String raw) {
        String sanitized = raw.replaceAll("[^a-zA-Z0-9_-]", "").toLowerCase();
        return sanitized.isEmpty() ? "user" : sanitized;
    }

    private OAuthProperties.ProviderConfig requireProvider(String provider) {
        if (!providers.containsKey(provider)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Unknown provider: " + provider);
        }
        Map<String, OAuthProperties.ProviderConfig> providerConfigs =
                oAuthProperties.getProviders();
        if (providerConfigs == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Missing OAuth configuration for provider: " + provider);
        }
        OAuthProperties.ProviderConfig config = providerConfigs.get(provider);
        if (config == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Missing OAuth configuration for provider: " + provider);
        }
        return config;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
