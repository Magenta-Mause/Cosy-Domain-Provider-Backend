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

    @Value("${frontend.url}")
    private String frontendUrl;

    @Override
    public void sendUserAccessToken(UserEntity user) {
        String verifyLink = frontendUrl + "/verify?token=" + user.getAccessToken();
        mailApiClient.sendEmail(SendMailDto.builder()
                        .recipient(user.getEmail())
                        .subject("[ COSY DOMAIN PROVIDER ] Verify Your Account")
                        .body(buildVerificationEmail(user.getUsername(), formatTokenForDisplay(user.getAccessToken()), verifyLink))
                        .enableHtml(true)
                        .build())
                .subscribe(response -> {
                    if (response.isSuccess()) {
                        System.out.println("Email sent successfully to " + user.getEmail());
                    } else {
                        System.err.println("Failed to send email to " + user.getEmail());
                    }
                }, error -> {
                    System.err.println("Error sending email to " + user.getEmail() + ": " + error.getMessage());
                });
    }

    @Override
    public void sendPasswordResetEmail(UserEntity user, String resetToken) {
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
        mailApiClient.sendEmail(SendMailDto.builder()
                        .recipient(user.getEmail())
                        .subject("[ COSY DOMAIN PROVIDER ] Reset Your Password")
                        .body(buildPasswordResetEmail(user.getUsername(), resetLink))
                        .enableHtml(true)
                        .build())
                .subscribe(response -> {
                    if (response.isSuccess()) {
                        System.out.println("Password reset email sent to " + user.getEmail());
                    } else {
                        System.err.println("Failed to send password reset email to " + user.getEmail());
                    }
                }, error -> {
                    System.err.println("Error sending password reset email to " + user.getEmail() + ": " + error.getMessage());
                });
    }

    private static final String DASH_SPAN = "<span style=\"user-select:none;-webkit-user-select:none;\">-</span>";

    private String formatTokenForDisplay(String token) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < token.length(); i += 2) {
            if (!sb.isEmpty()) {
                sb.append(DASH_SPAN);
            }
            sb.append(token, i, Math.min(i + 2, token.length()));
        }
        return sb.toString();
    }

    private String buildVerificationEmail(String username, String accessToken, String verifyLink) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <title>Verify Your Account</title>
                  <style>
                    @import url('https://fonts.googleapis.com/css2?family=Press+Start+2P&family=VT323:wght@400&display=swap');
                    body { margin: 0; padding: 0; background-color: #d4b896; font-family: 'VT323', monospace; }
                    .wrapper { max-width: 600px; margin: 40px auto; background-color: #e8d5b7; border: 3px solid #5c3d2e; box-shadow: 6px 6px 0 #5c3d2e; border-radius: 8px; overflow: hidden; }
                    .header { background-color: #5c3d2e; padding: 28px 32px; text-align: center; }
                    .header h1 { margin: 0; font-family: 'Press Start 2P', monospace; font-size: 14px; color: #e8d5b7; line-height: 1.8; letter-spacing: 1px; }
                    .header .pixel-icon { font-size: 32px; display: block; margin-bottom: 12px; }
                    .body { padding: 32px; }
                    .greeting { font-family: 'Press Start 2P', monospace; font-size: 11px; color: #5c3d2e; margin-bottom: 20px; line-height: 2; }
                    .message { font-size: 20px; color: #3d2b1a; line-height: 1.6; margin-bottom: 28px; }
                    .token-label { font-family: 'Press Start 2P', monospace; font-size: 9px; color: #5c3d2e; text-transform: uppercase; letter-spacing: 2px; margin-bottom: 10px; }
                    .token-box { background-color: #2a1a0e; border: 2px solid #5c3d2e; box-shadow: 4px 4px 0 #3d2b1a; border-radius: 4px; padding: 18px 20px; margin-bottom: 28px; }
                    .token-box code { font-family: 'VT323', monospace; font-size: 22px; color: #7ecac3; word-break: break-all; letter-spacing: 1px; }
                    .cta-btn { display: inline-block; background-color: #5c3d2e; color: #e8d5b7 !important; font-family: 'Press Start 2P', monospace; font-size: 10px; padding: 14px 24px; text-decoration: none; border-radius: 4px; box-shadow: 4px 4px 0 #3d2b1a; margin-bottom: 28px; }
                    .divider { border: none; border-top: 2px dashed #b09070; margin: 24px 0; }
                    .footer { background-color: #c9a97d; border-top: 3px solid #5c3d2e; padding: 20px 32px; text-align: center; }
                    .footer p { margin: 0; font-size: 18px; color: #5c3d2e; line-height: 1.6; }
                    .footer .brand { font-family: 'Press Start 2P', monospace; font-size: 8px; color: #3d2b1a; margin-top: 10px; display: block; letter-spacing: 1px; }
                  </style>
                </head>
                <body>
                  <div class="wrapper">
                    <div class="header">
                      <span class="pixel-icon">&#128274;</span>
                      <h1>VERIFY YOUR<br/>MAILBOX</h1>
                    </div>
                    <div class="body">
                      <p class="greeting">&gt; HELLO, %s</p>
                      <p class="message">
                        Welcome to Cosy Domain Provider!<br/>
                        Click the button below to verify your email address, or enter the code manually.
                      </p>
                      <div style="text-align:center; margin-bottom: 28px;">
                        <a href="%s" class="cta-btn">Verify my email &rarr;</a>
                      </div>
                      <p class="token-label">// OR ENTER THIS CODE MANUALLY</p>
                      <div class="token-box">
                        <code>%s</code>
                        <p style="font-size:14px; color:#7ecac3; margin:10px 0 0; font-family:'VT323',monospace; user-select:none; -webkit-user-select:none;">// dashes are visual only — copy gives clean token</p>
                      </div>
                      <hr class="divider" />
                      <p class="message">
                        If you did not create an account, you can safely ignore this email.
                      </p>
                    </div>
                    <div class="footer">
                      <p>Happy domain managing! &#127968;</p>
                      <span class="brand">COSY DOMAIN PROVIDER &mdash; &copy; 2026</span>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(username, verifyLink, accessToken);
    }

    private String buildPasswordResetEmail(String username, String resetLink) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <title>Reset Your Password</title>
                  <style>
                    @import url('https://fonts.googleapis.com/css2?family=Press+Start+2P&family=VT323:wght@400&display=swap');
                    body { margin: 0; padding: 0; background-color: #d4b896; font-family: 'VT323', monospace; }
                    .wrapper { max-width: 600px; margin: 40px auto; background-color: #e8d5b7; border: 3px solid #5c3d2e; box-shadow: 6px 6px 0 #5c3d2e; border-radius: 8px; overflow: hidden; }
                    .header { background-color: #5c3d2e; padding: 28px 32px; text-align: center; }
                    .header h1 { margin: 0; font-family: 'Press Start 2P', monospace; font-size: 14px; color: #e8d5b7; line-height: 1.8; letter-spacing: 1px; }
                    .header .pixel-icon { font-size: 32px; display: block; margin-bottom: 12px; }
                    .body { padding: 32px; }
                    .greeting { font-family: 'Press Start 2P', monospace; font-size: 11px; color: #5c3d2e; margin-bottom: 20px; line-height: 2; }
                    .message { font-size: 20px; color: #3d2b1a; line-height: 1.6; margin-bottom: 28px; }
                    .cta-btn { display: inline-block; background-color: #5c3d2e; color: #e8d5b7 !important; font-family: 'Press Start 2P', monospace; font-size: 10px; padding: 14px 24px; text-decoration: none; border-radius: 4px; box-shadow: 4px 4px 0 #3d2b1a; margin-bottom: 28px; }
                    .warning-box { background-color: #fff3cd; border: 2px solid #c9973a; box-shadow: 3px 3px 0 #c9973a; border-radius: 4px; padding: 14px 18px; margin-bottom: 28px; font-size: 18px; color: #7a4e12; line-height: 1.5; }
                    .warning-box strong { font-family: 'Press Start 2P', monospace; font-size: 9px; display: block; margin-bottom: 6px; color: #c9973a; }
                    .divider { border: none; border-top: 2px dashed #b09070; margin: 24px 0; }
                    .footer { background-color: #c9a97d; border-top: 3px solid #5c3d2e; padding: 20px 32px; text-align: center; }
                    .footer p { margin: 0; font-size: 18px; color: #5c3d2e; line-height: 1.6; }
                    .footer .brand { font-family: 'Press Start 2P', monospace; font-size: 8px; color: #3d2b1a; margin-top: 10px; display: block; letter-spacing: 1px; }
                  </style>
                </head>
                <body>
                  <div class="wrapper">
                    <div class="header">
                      <span class="pixel-icon">&#128273;</span>
                      <h1>PASSWORD<br/>RESET</h1>
                    </div>
                    <div class="body">
                      <p class="greeting">&gt; HELLO, %s</p>
                      <p class="message">
                        We received a request to reset your password.<br/>
                        Click the button below to choose a new one. This link expires in 30 minutes.
                      </p>
                      <div style="text-align:center; margin-bottom: 28px;">
                        <a href="%s" class="cta-btn">Reset my password &rarr;</a>
                      </div>
                      <div class="warning-box">
                        <strong>!! HEADS UP !!</strong>
                        If you did not request a password reset, you can safely ignore this email.
                        Your password will not change.
                      </div>
                      <hr class="divider" />
                      <p class="message" style="font-size:16px; color:#6b5040;">
                        If the button doesn't work, copy this link into your browser:<br/>
                        <span style="color:#5c3d2e; word-break:break-all;">%s</span>
                      </p>
                    </div>
                    <div class="footer">
                      <p>Stay cosy! &#127968;</p>
                      <span class="brand">COSY DOMAIN PROVIDER &mdash; &copy; 2026</span>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(username, resetLink, resetLink);
    }
}
