package com.magentamause.cosydomainprovider.services.core;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.model.core.MfaSetupResponseDto;
import com.magentamause.cosydomainprovider.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class MfaServiceTest {

    @Mock private UserRepository userRepository;

    private MfaService mfaService;

    @BeforeEach
    void setUp() {
        mfaService = new MfaService(userRepository);
    }

    private UserEntity verifiedUser() {
        return UserEntity.builder()
                .uuid("u1")
                .username("alice")
                .email("alice@example.com")
                .isVerified(true)
                .isMfaEnabled(false)
                .build();
    }

    @Test
    void setupMfa_notVerified_throws() {
        UserEntity u = UserEntity.builder().uuid("u1").username("x").email("x@x.com").isVerified(false).build();
        assertThatThrownBy(() -> mfaService.setupMfa(u))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void setupMfa_alreadyEnabled_throws() {
        UserEntity u = verifiedUser();
        u.setMfaEnabled(true);
        assertThatThrownBy(() -> mfaService.setupMfa(u))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void setupMfa_success_returnsTotpUri() {
        UserEntity u = verifiedUser();
        when(userRepository.save(any())).thenReturn(u);

        MfaSetupResponseDto dto = mfaService.setupMfa(u);
        assertThat(dto.getTotpUri()).isNotBlank();
        assertThat(dto.getSecret()).isNotBlank();
        assertThat(u.getMfaSecret()).isNotNull();
        verify(userRepository).save(u);
    }

    @Test
    void confirmMfa_noSecret_throws() {
        UserEntity u = verifiedUser();
        u.setMfaSecret(null);
        assertThatThrownBy(() -> mfaService.confirmMfa(u, "123456"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void confirmMfa_invalidCode_throws() {
        UserEntity u = verifiedUser();
        u.setMfaSecret("JBSWY3DPEHPK3PXP");
        assertThatThrownBy(() -> mfaService.confirmMfa(u, "000000"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void verifyCode_invalidCode_returnsFalse() {
        assertThat(mfaService.verifyCode("JBSWY3DPEHPK3PXP", "000000")).isFalse();
    }
}
