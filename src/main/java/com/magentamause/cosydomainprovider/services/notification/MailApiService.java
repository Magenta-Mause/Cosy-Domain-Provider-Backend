package com.magentamause.cosydomainprovider.services.notification;

import com.magentamause.cosydomainprovider.client.MailApiClient;
import com.magentamause.cosydomainprovider.client.mail.model.SendMailDto;
import com.magentamause.cosydomainprovider.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
        String body =
                emailTemplateService.renderVerificationEmail(
                        user.getUsername(), user.getAccessToken(), verifyLink);
        mailApiClient
                .sendEmail(
                        SendMailDto.builder()
                                .recipient(user.getEmail())
                                .subject("[ COSY DOMAIN PROVIDER ] Verify Your Account")
                                .body(body)
                                .enableHtml(true)
                                .build())
                .subscribe(
                        response -> {
                            if (response.isSuccess()) {
                                System.out.println("Email sent successfully to " + user.getEmail());
                            } else {
                                System.err.println("Failed to send email to " + user.getEmail());
                            }
                        },
                        error ->
                                System.err.println(
                                        "Error sending email to "
                                                + user.getEmail()
                                                + ": "
                                                + error.getMessage()));
    }

    @Override
    public void sendPasswordResetEmail(UserEntity user, String resetToken) {
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
        String body = emailTemplateService.renderPasswordResetEmail(user.getUsername(), resetLink);
        mailApiClient
                .sendEmail(
                        SendMailDto.builder()
                                .recipient(user.getEmail())
                                .subject("[ COSY DOMAIN PROVIDER ] Reset Your Password")
                                .body(body)
                                .enableHtml(true)
                                .build())
                .subscribe(
                        response -> {
                            if (response.isSuccess()) {
                                System.out.println(
                                        "Password reset email sent to " + user.getEmail());
                            } else {
                                System.err.println(
                                        "Failed to send password reset email to "
                                                + user.getEmail());
                            }
                        },
                        error ->
                                System.err.println(
                                        "Error sending password reset email to "
                                                + user.getEmail()
                                                + ": "
                                                + error.getMessage()));
    }
}
