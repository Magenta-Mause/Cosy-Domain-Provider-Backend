package com.magentamause.cosydomainprovider.services.auth.oauth;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.magentamause.cosydomainprovider.configuration.oauth.OAuthProperties;
import com.magentamause.cosydomainprovider.entity.OAuthIdentityEntity;
import com.magentamause.cosydomainprovider.entity.OAuthStateEntity;
import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.repository.OAuthIdentityRepository;
import com.magentamause.cosydomainprovider.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OAuthServiceTest {

    @Mock private OAuthProperties oAuthProperties;
    @Mock private OAuthIdentityRepository oAuthIdentityRepository;
    @Mock private UserRepository userRepository;
    @Mock private OAuthStateStore stateStore;
    @Mock private WebClient webClient;
    @Mock private OAuthProviderClient githubProvider;

    private OAuthService service;

    private OAuthProperties.ProviderConfig githubConfig() {
        OAuthProperties.ProviderConfig config = new OAuthProperties.ProviderConfig();
        config.setClientId("client123");
        config.setClientSecret("secret456");
        config.setAuthorizationUri("https://github.com/login/oauth/authorize");
        config.setTokenUri("https://github.com/login/oauth/access_token");
        config.setUserInfoUri("https://api.github.com/user");
        config.setScope("read:user user:email");
        config.setCallbackUri("http://localhost:8080/api/v1/auth/oauth/github/callback");
        return config;
    }

    @BeforeEach
    void setUp() {
        when(githubProvider.providerName()).thenReturn("github");
        when(oAuthProperties.getProviders()).thenReturn(Map.of("github", githubConfig()));
        service =
                new OAuthService(
                        oAuthProperties,
                        oAuthIdentityRepository,
                        userRepository,
                        stateStore,
                        webClient,
                        List.of(githubProvider));
    }

    @Test
    void buildAuthorizationUrl_containsRequiredParams() {
        when(stateStore.generateState()).thenReturn("state-xyz");

        String url = service.buildAuthorizationUrl("github");

        assertThat(url)
                .contains("https://github.com/login/oauth/authorize")
                .contains("client_id=client123")
                .contains("state=state-xyz")
                .contains("response_type=code");
    }

    @Test
    void buildAuthorizationUrl_unknownProvider_throwsBadRequest() {
        assertThatThrownBy(() -> service.buildAuthorizationUrl("unknown"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(
                        e ->
                                assertThat(((ResponseStatusException) e).getStatusCode())
                                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private OAuthStateEntity loginState() {
        return OAuthStateEntity.builder()
                .state("valid-state")
                .issuedAt(Instant.now())
                .linkedUserId(null)
                .build();
    }

    @Test
    void handleLoginCallback_invalidState_throwsBadRequest() {
        when(stateStore.consumeState("bad-state")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handleLoginCallback("github", "code123", "bad-state"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(
                        e ->
                                assertThat(((ResponseStatusException) e).getStatusCode())
                                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void handleLoginCallback_existingOAuthIdentity_returnsLinkedUser() {
        UserEntity user =
                UserEntity.builder().uuid("u1").username("alice").email("a@a.com").build();
        OAuthIdentityEntity identity =
                OAuthIdentityEntity.builder()
                        .user(user)
                        .provider("github")
                        .providerSubject("gh-123")
                        .build();

        when(stateStore.consumeState("valid-state")).thenReturn(Optional.of(loginState()));
        mockTokenExchange("github-access-token");
        when(githubProvider.fetchUserInfo(eq("github-access-token"), any(), any()))
                .thenReturn(new OAuthUserInfo("gh-123", "a@a.com", "alice"));
        when(oAuthIdentityRepository.findByProviderAndProviderSubject("github", "gh-123"))
                .thenReturn(Optional.of(identity));

        UserEntity result = service.handleLoginCallback("github", "code123", "valid-state");

        assertThat(result.getUuid()).isEqualTo("u1");
        verify(oAuthIdentityRepository, never()).save(any());
    }

    @Test
    void handleLoginCallback_newOAuthUser_createsUserAndIdentity() {
        when(stateStore.consumeState("valid-state")).thenReturn(Optional.of(loginState()));
        mockTokenExchange("github-access-token");
        when(githubProvider.fetchUserInfo(eq("github-access-token"), any(), any()))
                .thenReturn(new OAuthUserInfo("gh-456", "new@example.com", "newuser"));
        when(oAuthIdentityRepository.findByProviderAndProviderSubject("github", "gh-456"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsernameIgnoreCase("newuser")).thenReturn(Optional.empty());
        UserEntity created =
                UserEntity.builder()
                        .uuid("u2")
                        .username("newuser")
                        .email("new@example.com")
                        .build();
        when(userRepository.save(any())).thenReturn(created);
        when(oAuthIdentityRepository.save(any())).thenReturn(mock(OAuthIdentityEntity.class));

        UserEntity result = service.handleLoginCallback("github", "code123", "valid-state");

        assertThat(result.getUuid()).isEqualTo("u2");
        verify(oAuthIdentityRepository).save(any());
    }

    @Test
    void handleLoginCallback_existingUserByEmail_throwsConflict() {
        UserEntity existingUser =
                UserEntity.builder().uuid("u3").username("bob").email("bob@example.com").build();
        when(stateStore.consumeState("valid-state")).thenReturn(Optional.of(loginState()));
        mockTokenExchange("github-access-token");
        when(githubProvider.fetchUserInfo(eq("github-access-token"), any(), any()))
                .thenReturn(new OAuthUserInfo("gh-789", "bob@example.com", "bob"));
        when(oAuthIdentityRepository.findByProviderAndProviderSubject("github", "gh-789"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("bob@example.com"))
                .thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> service.handleLoginCallback("github", "code123", "valid-state"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(
                        e ->
                                assertThat(((ResponseStatusException) e).getStatusCode())
                                        .isEqualTo(HttpStatus.CONFLICT));
        verify(oAuthIdentityRepository, never()).save(any());
    }

    @Test
    void handleLoginCallback_usernameConflict_appendsRandomSuffix() {
        when(stateStore.consumeState("valid-state")).thenReturn(Optional.of(loginState()));
        mockTokenExchange("github-access-token");
        when(githubProvider.fetchUserInfo(eq("github-access-token"), any(), any()))
                .thenReturn(new OAuthUserInfo("gh-999", "alice2@example.com", "alice"));
        when(oAuthIdentityRepository.findByProviderAndProviderSubject("github", "gh-999"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("alice2@example.com"))
                .thenReturn(Optional.empty());
        when(userRepository.findByUsernameIgnoreCase("alice"))
                .thenReturn(Optional.of(mock(UserEntity.class)));
        UserEntity created =
                UserEntity.builder()
                        .uuid("u4")
                        .username("alice_abc123")
                        .email("alice2@example.com")
                        .build();
        when(userRepository.save(any())).thenReturn(created);
        when(oAuthIdentityRepository.save(any())).thenReturn(mock(OAuthIdentityEntity.class));

        UserEntity result = service.handleLoginCallback("github", "code123", "valid-state");

        assertThat(result.getUuid()).isEqualTo("u4");
    }

    private OAuthStateEntity linkState(String userId) {
        return OAuthStateEntity.builder()
                .state("valid-state")
                .issuedAt(Instant.now())
                .linkedUserId(userId)
                .build();
    }

    @Test
    void buildLinkAuthorizationUrl_containsStateAndParams() {
        when(stateStore.generateLinkState("u1")).thenReturn("link-state-xyz");

        String url = service.buildLinkAuthorizationUrl("github", "u1");

        assertThat(url)
                .contains("https://github.com/login/oauth/authorize")
                .contains("state=link-state-xyz")
                .contains("client_id=client123");
    }

    @Test
    void handleLinkCallback_invalidState_throwsBadRequest() {
        when(stateStore.consumeState("bad")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handleLinkCallback("github", "code", "bad"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(
                        e ->
                                assertThat(((ResponseStatusException) e).getStatusCode())
                                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void handleLinkCallback_loginStatePassedToLinkEndpoint_throwsBadRequest() {
        when(stateStore.consumeState("login-state")).thenReturn(Optional.of(loginState()));

        assertThatThrownBy(() -> service.handleLinkCallback("github", "code", "login-state"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(
                        e ->
                                assertThat(((ResponseStatusException) e).getStatusCode())
                                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void handleLinkCallback_alreadyLinked_throwsConflict() {
        when(stateStore.consumeState("valid-state")).thenReturn(Optional.of(linkState("u1")));
        mockTokenExchange("github-access-token");
        when(githubProvider.fetchUserInfo(eq("github-access-token"), any(), any()))
                .thenReturn(new OAuthUserInfo("gh-100", "a@a.com", "alice"));
        when(oAuthIdentityRepository.findByProviderAndProviderSubject("github", "gh-100"))
                .thenReturn(
                        Optional.of(
                                mock(
                                        com.magentamause.cosydomainprovider.entity
                                                .OAuthIdentityEntity.class)));

        assertThatThrownBy(() -> service.handleLinkCallback("github", "code", "valid-state"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(
                        e ->
                                assertThat(((ResponseStatusException) e).getStatusCode())
                                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void handleLinkCallback_success_savesIdentity() {
        UserEntity user =
                UserEntity.builder().uuid("u1").username("alice").email("a@a.com").build();
        when(stateStore.consumeState("valid-state")).thenReturn(Optional.of(linkState("u1")));
        mockTokenExchange("github-access-token");
        when(githubProvider.fetchUserInfo(eq("github-access-token"), any(), any()))
                .thenReturn(new OAuthUserInfo("gh-200", "a@a.com", "alice"));
        when(oAuthIdentityRepository.findByProviderAndProviderSubject("github", "gh-200"))
                .thenReturn(Optional.empty());
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        service.handleLinkCallback("github", "code", "valid-state");

        verify(oAuthIdentityRepository).save(any());
    }

    @Test
    void getLinkedIdentities_returnsAllForUser() {
        com.magentamause.cosydomainprovider.entity.OAuthIdentityEntity identity =
                com.magentamause.cosydomainprovider.entity.OAuthIdentityEntity.builder()
                        .provider("github")
                        .email("a@a.com")
                        .build();
        when(oAuthIdentityRepository.findAllByUser_Uuid("u1")).thenReturn(List.of(identity));

        List<com.magentamause.cosydomainprovider.model.core.OAuthIdentityDto> result =
                service.getLinkedIdentities("u1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProvider()).isEqualTo("github");
    }

    @Test
    void unlinkIdentity_withPasswordAndMultipleIdentities_succeeds() {
        UserEntity user =
                UserEntity.builder()
                        .uuid("u1")
                        .username("alice")
                        .email("a@a.com")
                        .passwordHash("hash")
                        .build();
        com.magentamause.cosydomainprovider.entity.OAuthIdentityEntity identity =
                com.magentamause.cosydomainprovider.entity.OAuthIdentityEntity.builder()
                        .provider("github")
                        .email("a@a.com")
                        .build();
        when(oAuthIdentityRepository.findByProviderAndUser_Uuid("github", "u1"))
                .thenReturn(Optional.of(identity));
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(oAuthIdentityRepository.countByUser_Uuid("u1")).thenReturn(2L);

        service.unlinkIdentity("github", "u1");

        verify(oAuthIdentityRepository).delete(identity);
    }

    @Test
    void unlinkIdentity_noPasswordLastIdentity_throwsConflict() {
        UserEntity user =
                UserEntity.builder()
                        .uuid("u1")
                        .username("alice")
                        .email("a@a.com")
                        .passwordHash(null)
                        .build();
        com.magentamause.cosydomainprovider.entity.OAuthIdentityEntity identity =
                com.magentamause.cosydomainprovider.entity.OAuthIdentityEntity.builder()
                        .provider("github")
                        .email("a@a.com")
                        .build();
        when(oAuthIdentityRepository.findByProviderAndUser_Uuid("github", "u1"))
                .thenReturn(Optional.of(identity));
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(oAuthIdentityRepository.countByUser_Uuid("u1")).thenReturn(1L);

        assertThatThrownBy(() -> service.unlinkIdentity("github", "u1"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(
                        e ->
                                assertThat(((ResponseStatusException) e).getStatusCode())
                                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void unlinkIdentity_notFound_throwsNotFound() {
        when(oAuthIdentityRepository.findByProviderAndUser_Uuid("github", "u1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.unlinkIdentity("github", "u1"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(
                        e ->
                                assertThat(((ResponseStatusException) e).getStatusCode())
                                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void mockTokenExchange(String token) {
        WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.accept(any())).thenReturn(bodySpec);
        when(bodySpec.body(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class))
                .thenReturn(Mono.just(Map.of("access_token", token)));
    }
}
