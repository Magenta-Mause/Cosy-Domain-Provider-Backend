package com.magentamause.cosydomainprovider.services.notification;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.magentamause.cosydomainprovider.client.MailApiClient;
import com.magentamause.cosydomainprovider.client.mail.model.MailEntityResponse;
import com.magentamause.cosydomainprovider.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class MailApiServiceTest {

    @Mock private MailApiClient mailApiClient;
    @Mock private EmailTemplateService emailTemplateService;

    private MailApiService service;

    @BeforeEach
    void setUp() {
        service = new MailApiService(mailApiClient, emailTemplateService);
        ReflectionTestUtils.setField(service, "frontendUrl", "http://localhost:5173");
    }

    private UserEntity user() {
        return UserEntity.builder()
                .uuid("u1")
                .username("alice")
                .email("alice@example.com")
                .accessToken("TOKEN123")
                .build();
    }

    @Test
    void sendUserAccessToken_sendsEmail() {
        UserEntity u = user();
        when(emailTemplateService.renderVerificationEmail(any(), any(), any()))
                .thenReturn("<html>verify</html>");
        MailEntityResponse successResp = mock(MailEntityResponse.class);
        when(successResp.isSuccess()).thenReturn(true);
        when(mailApiClient.sendEmail(any())).thenReturn(Mono.just(successResp));

        service.sendUserAccessToken(u);

        verify(emailTemplateService)
                .renderVerificationEmail(eq("alice"), eq("TOKEN123"), contains("TOKEN123"));
        verify(mailApiClient).sendEmail(any());
    }

    @Test
    void sendUserAccessToken_emailFailure_logsError() {
        UserEntity u = user();
        when(emailTemplateService.renderVerificationEmail(any(), any(), any()))
                .thenReturn("<html>verify</html>");
        MailEntityResponse failResp = mock(MailEntityResponse.class);
        when(failResp.isSuccess()).thenReturn(false);
        when(mailApiClient.sendEmail(any())).thenReturn(Mono.just(failResp));

        service.sendUserAccessToken(u);

        verify(mailApiClient).sendEmail(any());
    }

    @Test
    void sendUserAccessToken_clientError_logsError() {
        UserEntity u = user();
        when(emailTemplateService.renderVerificationEmail(any(), any(), any()))
                .thenReturn("<html>verify</html>");
        when(mailApiClient.sendEmail(any()))
                .thenReturn(Mono.error(new RuntimeException("network error")));

        service.sendUserAccessToken(u);

        verify(mailApiClient).sendEmail(any());
    }

    @Test
    void sendPasswordResetEmail_sendsEmail() {
        UserEntity u = user();
        when(emailTemplateService.renderPasswordResetEmail(any(), any()))
                .thenReturn("<html>reset</html>");
        MailEntityResponse successResp = mock(MailEntityResponse.class);
        when(successResp.isSuccess()).thenReturn(true);
        when(mailApiClient.sendEmail(any())).thenReturn(Mono.just(successResp));

        service.sendPasswordResetEmail(u, "reset-token-abc");

        verify(emailTemplateService)
                .renderPasswordResetEmail(eq("alice"), contains("reset-token-abc"));
        verify(mailApiClient).sendEmail(any());
    }

    @Test
    void sendPasswordResetEmail_emailFailure_logsError() {
        UserEntity u = user();
        when(emailTemplateService.renderPasswordResetEmail(any(), any()))
                .thenReturn("<html>reset</html>");
        MailEntityResponse failResp = mock(MailEntityResponse.class);
        when(failResp.isSuccess()).thenReturn(false);
        when(mailApiClient.sendEmail(any())).thenReturn(Mono.just(failResp));

        service.sendPasswordResetEmail(u, "reset-token");

        verify(mailApiClient).sendEmail(any());
    }

    @Test
    void sendPasswordResetEmail_clientError_logsError() {
        UserEntity u = user();
        when(emailTemplateService.renderPasswordResetEmail(any(), any()))
                .thenReturn("<html>reset</html>");
        when(mailApiClient.sendEmail(any()))
                .thenReturn(Mono.error(new RuntimeException("timeout")));

        service.sendPasswordResetEmail(u, "reset-token");

        verify(mailApiClient).sendEmail(any());
    }
}
