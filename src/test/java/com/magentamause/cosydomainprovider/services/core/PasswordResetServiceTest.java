package com.magentamause.cosydomainprovider.services.core;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.magentamause.cosydomainprovider.entity.UserEntity;
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
class PasswordResetServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock private MessagingService messagingService;

    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(userRepository, passwordEncoder, messagingService);
    }

    @Test
    void initiatePasswordReset_userNotFound_silentNoOp() {
        when(userRepository.findByEmailIgnoreCase("x@x.com")).thenReturn(Optional.empty());
        assertThatNoException().isThrownBy(() -> service.initiatePasswordReset("x@x.com"));
        verify(messagingService, never()).sendPasswordResetEmail(any(), any());
    }

    @Test
    void initiatePasswordReset_success_savesAndSendsEmail() {
        UserEntity u = UserEntity.builder().uuid("u1").username("a").email("a@a.com").build();
        when(userRepository.findByEmailIgnoreCase("a@a.com")).thenReturn(Optional.of(u));
        when(userRepository.save(any())).thenReturn(u);

        service.initiatePasswordReset("a@a.com");
        assertThat(u.getPasswordResetToken()).isNotNull();
        assertThat(u.getPasswordResetExpiresAt()).isAfter(Instant.now());
        verify(messagingService).sendPasswordResetEmail(eq(u), any());
    }

    @Test
    void confirmPasswordReset_invalidToken_throws() {
        when(userRepository.findByPasswordResetToken("bad")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.confirmPasswordReset("bad", "newpass"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void confirmPasswordReset_expired_throws() {
        UserEntity u = UserEntity.builder().uuid("u1").username("a").email("a@a.com").build();
        u.setPasswordResetToken("tok");
        u.setPasswordResetExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        when(userRepository.findByPasswordResetToken("tok")).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> service.confirmPasswordReset("tok", "newpass"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void confirmPasswordReset_nullExpiry_throws() {
        UserEntity u = UserEntity.builder().uuid("u1").username("a").email("a@a.com").build();
        u.setPasswordResetToken("tok");
        u.setPasswordResetExpiresAt(null);
        when(userRepository.findByPasswordResetToken("tok")).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> service.confirmPasswordReset("tok", "newpass"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void confirmPasswordReset_success() {
        UserEntity u = UserEntity.builder().uuid("u1").username("a").email("a@a.com").build();
        u.setPasswordResetToken("tok");
        u.setPasswordResetExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
        u.setMfaEnabled(true);
        u.setMfaSecret("secret");
        when(userRepository.findByPasswordResetToken("tok")).thenReturn(Optional.of(u));
        when(passwordEncoder.encode("newpass")).thenReturn("hash");
        when(userRepository.save(any())).thenReturn(u);

        service.confirmPasswordReset("tok", "newpass");
        assertThat(u.getPasswordHash()).isEqualTo("hash");
        assertThat(u.getPasswordResetToken()).isNull();
        assertThat(u.isMfaEnabled()).isFalse();
        assertThat(u.getMfaSecret()).isNull();
    }
}
