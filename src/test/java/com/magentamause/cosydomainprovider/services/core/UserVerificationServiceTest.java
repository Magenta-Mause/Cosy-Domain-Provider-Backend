package com.magentamause.cosydomainprovider.services.core;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.model.exception.UserNotFoundException;
import com.magentamause.cosydomainprovider.repository.UserRepository;
import com.magentamause.cosydomainprovider.services.notification.MessagingService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class UserVerificationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private MessagingService messagingService;

    private UserVerificationService service;

    @BeforeEach
    void setUp() {
        service = new UserVerificationService(userRepository, messagingService);
    }

    private UserEntity unverifiedUser(String token) {
        return UserEntity.builder()
                .uuid("u1")
                .username("alice")
                .email("alice@example.com")
                .isVerified(false)
                .accessToken(token)
                .accessTokenExpiresAt(Instant.now().plus(3, ChronoUnit.HOURS))
                .build();
    }

    @Test
    void sendInitialVerification_setsTokenAndSaves() {
        UserEntity u = UserEntity.builder().uuid("u1").username("a").email("a@a.com").build();
        when(userRepository.save(any())).thenReturn(u);

        service.sendInitialVerification(u);
        assertThat(u.getAccessToken()).isNotNull();
        assertThat(u.getAccessTokenExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void resendVerificationCode_userNotFound_throws() {
        when(userRepository.findById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.resendVerificationCode("x"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void resendVerificationCode_alreadyVerified_throws() {
        UserEntity u = UserEntity.builder().uuid("u1").username("a").email("a@a.com").isVerified(true).build();
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));
        assertThatThrownBy(() -> service.resendVerificationCode("u1"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void resendVerificationCode_success_sendsEmail() {
        UserEntity u = unverifiedUser("OLDTOK");
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));
        when(userRepository.save(any())).thenReturn(u);

        service.resendVerificationCode("u1");
        assertThat(u.getAccessToken()).isNotEqualTo("OLDTOK");
        verify(messagingService).sendUserAccessToken(u);
    }

    @Test
    void verifyUser_success() {
        UserEntity u = unverifiedUser("ABC123");
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));
        when(userRepository.save(any())).thenReturn(u);

        service.verifyUser("u1", "ABC123");
        assertThat(u.isVerified()).isTrue();
    }

    @Test
    void verifyUser_notFound_throws() {
        when(userRepository.findById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.verifyUser("x", "tok"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void verifyUser_expired_throws() {
        UserEntity u = unverifiedUser("ABC123");
        u.setAccessTokenExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> service.verifyUser("u1", "ABC123"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void verifyUser_wrongToken_throws() {
        UserEntity u = unverifiedUser("ABC123");
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> service.verifyUser("u1", "WRONG1"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void verifyUserByToken_success() {
        UserEntity u = unverifiedUser("ABC123");
        when(userRepository.findByAccessToken("ABC123")).thenReturn(Optional.of(u));
        when(userRepository.save(any())).thenReturn(u);

        service.verifyUserByToken("ABC123");
        assertThat(u.isVerified()).isTrue();
    }

    @Test
    void verifyUserByToken_notFound_throws() {
        when(userRepository.findByAccessToken("BAD")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.verifyUserByToken("BAD"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void verifyUserByToken_stripsHyphens() {
        UserEntity u = unverifiedUser("ABC123");
        when(userRepository.findByAccessToken("ABC123")).thenReturn(Optional.of(u));
        when(userRepository.save(any())).thenReturn(u);

        service.verifyUserByToken("ABC-123");
        assertThat(u.isVerified()).isTrue();
    }
}
