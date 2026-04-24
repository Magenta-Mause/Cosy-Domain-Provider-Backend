package com.magentamause.cosydomainprovider.services.notification;

import com.magentamause.cosydomainprovider.client.MailApiClient;
import com.magentamause.cosydomainprovider.client.mail.model.SendMailDto;
import com.magentamause.cosydomainprovider.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailApiService implements MessagingService {

    private final MailApiClient mailApiClient;
    private final EmailTemplateService emailTemplateService;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Override
    public void sendUserAccessToken(UserEntity user) {
        String verifyLink = frontendUrl + "/verify?token=" + user.getAccessToken();
        String body = emailTemplateService.renderVerificationEmail(user.getUsername(), user.getAccessToken(), verifyLink);
        mailApiClient.sendEmail(SendMailDto.builder()
                        .recipient(user.getEmail())
                        .subject("[ COSY DOMAIN PROVIDER ] Verify Your Account")
                        .body(body)
                        .enableHtml(true)
                        .build())
                .subscribe(
                        response -> {
                            if (response.isSuccess()) {
                                log.info("Email sent successfully to {}", user.getEmail());
                            } else {
                                log.error("Failed to send email to {}, {}", user.getEmail(), response);
                            }
                        },
                        error -> log.error("Error sending email to {}: {}", user.getEmail(), error.getMessage()));
    }

    @Override
    public void sendPasswordResetEmail(UserEntity user, String resetToken) {
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
        String body = emailTemplateService.renderPasswordResetEmail(user.getUsername(), resetLink);
        mailApiClient.sendEmail(SendMailDto.builder()
                        .recipient(user.getEmail())
                        .subject("[ COSY DOMAIN PROVIDER ] Reset Your Password")
                        .body(body)
                        .enableHtml(true)
                        .build())
                .subscribe(
                        response -> {
                            if (response.isSuccess()) {
                                log.info("Password reset email sent to {}", user.getEmail());
                            } else {
                                log.error("Failed to send password reset email to {}", user.getEmail());
                            }
                        },
                        error -> System.err.println("Error sending password reset email to " + user.getEmail() + ": " + error.getMessage()));
    }
}
