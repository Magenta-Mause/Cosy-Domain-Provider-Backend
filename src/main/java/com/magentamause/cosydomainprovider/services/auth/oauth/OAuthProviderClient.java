package com.magentamause.cosydomainprovider.services.auth.oauth;

import org.springframework.web.reactive.function.client.WebClient;

public interface OAuthProviderClient {

    String providerName();

    OAuthUserInfo fetchUserInfo(String accessToken, WebClient webClient, String userInfoUri);
}
