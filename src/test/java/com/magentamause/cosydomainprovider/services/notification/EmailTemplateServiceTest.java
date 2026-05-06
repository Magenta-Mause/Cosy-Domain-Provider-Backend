package com.magentamause.cosydomainprovider.services.notification;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

class EmailTemplateServiceTest {

    private TemplateEngine templateEngine;
    private EmailTemplateService service;

    @BeforeEach
    void setUp() {
        templateEngine = mock(TemplateEngine.class);
        service = new EmailTemplateService(templateEngine);
    }

    @Test
    void renderVerificationEmail_callsTemplateEngine() {
        when(templateEngine.process(eq("mail/verification-email"), any(Context.class)))
                .thenReturn("<html>verification</html>");

        String result = service.renderVerificationEmail("alice", "ABCD1234", "https://verify.link");

        assertThat(result).isEqualTo("<html>verification</html>");
        verify(templateEngine).process(eq("mail/verification-email"), any(Context.class));
    }

    @Test
    void renderPasswordResetEmail_callsTemplateEngine() {
        when(templateEngine.process(eq("mail/password-reset-email"), any(Context.class)))
                .thenReturn("<html>reset</html>");

        String result = service.renderPasswordResetEmail("bob", "https://reset.link");

        assertThat(result).isEqualTo("<html>reset</html>");
        verify(templateEngine).process(eq("mail/password-reset-email"), any(Context.class));
    }

    @Test
    void renderVerificationEmail_formatsTokenWithDashes() {
        // Capture context to verify token formatting (AB-CD-12-34)
        when(templateEngine.process(eq("mail/verification-email"), any(Context.class)))
                .thenAnswer(
                        invocation -> {
                            Context ctx = invocation.getArgument(1);
                            return ctx.getVariable("accessToken").toString();
                        });

        String result = service.renderVerificationEmail("alice", "ABCD12", "https://link");

        assertThat(result).isEqualTo("AB-CD-12");
    }

    @Test
    void renderVerificationEmail_oddLengthToken_handledCorrectly() {
        when(templateEngine.process(eq("mail/verification-email"), any(Context.class)))
                .thenAnswer(
                        invocation -> {
                            Context ctx = invocation.getArgument(1);
                            return ctx.getVariable("accessToken").toString();
                        });

        String result = service.renderVerificationEmail("alice", "ABC", "https://link");

        assertThat(result).isEqualTo("AB-C");
    }
}
