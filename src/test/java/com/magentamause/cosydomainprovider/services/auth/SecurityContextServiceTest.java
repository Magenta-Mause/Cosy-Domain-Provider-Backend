package com.magentamause.cosydomainprovider.services.auth;

import static org.assertj.core.api.Assertions.*;

import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.security.jwtfilter.AuthenticationToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityContextServiceTest {

    private final SecurityContextService service = new SecurityContextService();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAuthenticationToken_noAuth_returnsNull() {
        assertThat(service.getAuthenticationToken()).isNull();
    }

    @Test
    void getAuthenticationToken_wrongType_returnsNull() {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new org.springframework.security.authentication
                                .UsernamePasswordAuthenticationToken("user", "pass"));
        assertThat(service.getAuthenticationToken()).isNull();
    }

    @Test
    void getAuthenticationToken_validToken_returnsToken() {
        UserEntity user = UserEntity.builder().uuid("u1").username("alice").build();
        AuthenticationToken token = new AuthenticationToken("u1", user);
        SecurityContextHolder.getContext().setAuthentication(token);

        assertThat(service.getAuthenticationToken()).isSameAs(token);
    }

    @Test
    void getUserId_noAuth_returnsNull() {
        assertThat(service.getUserId()).isNull();
    }

    @Test
    void getUserId_withAuth_returnsId() {
        UserEntity user = UserEntity.builder().uuid("u1").username("alice").build();
        SecurityContextHolder.getContext().setAuthentication(new AuthenticationToken("u1", user));

        assertThat(service.getUserId()).isEqualTo("u1");
    }

    @Test
    void getUser_noAuth_returnsNull() {
        assertThat(service.getUser()).isNull();
    }

    @Test
    void getUser_withAuth_returnsUser() {
        UserEntity user = UserEntity.builder().uuid("u1").username("alice").build();
        SecurityContextHolder.getContext().setAuthentication(new AuthenticationToken("u1", user));

        assertThat(service.getUser()).isSameAs(user);
    }
}
