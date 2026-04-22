package com.magentamause.cosydomainprovider.services.notification;

import com.magentamause.cosydomainprovider.client.MailApiClient;
import com.magentamause.cosydomainprovider.client.mail.model.SendMailDto;
import com.magentamause.cosydomainprovider.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailApiService implements MessagingService {
    private final MailApiClient mailApiClient;

    @Override
    public void sendUserAccessToken(UserEntity user) {
        mailApiClient.sendEmail(SendMailDto.builder()
                        .recipient(user.getEmail())
                        .subject("[ COSY DOMAIN PROVIDER ] Your Access Token")
                        .body(buildAccessTokenEmail(user.getUsername(), user.getAccessToken()))
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

    private String buildAccessTokenEmail(String username, String accessToken) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <title>Your Access Token</title>
                  <style>
                    @import url('https://fonts.googleapis.com/css2?family=Press+Start+2P&family=VT323:wght@400&display=swap');

                    body {
                      margin: 0;
                      padding: 0;
                      background-color: #d4b896;
                      font-family: 'VT323', monospace;
                    }

                    .wrapper {
                      max-width: 600px;
                      margin: 40px auto;
                      background-color: #e8d5b7;
                      border: 3px solid #5c3d2e;
                      box-shadow: 6px 6px 0 #5c3d2e;
                      border-radius: 8px;
                      overflow: hidden;
                    }

                    .header {
                      background-color: #5c3d2e;
                      padding: 28px 32px;
                      text-align: center;
                    }

                    .header h1 {
                      margin: 0;
                      font-family: 'Press Start 2P', monospace;
                      font-size: 14px;
                      color: #e8d5b7;
                      line-height: 1.8;
                      letter-spacing: 1px;
                    }

                    .header .pixel-icon {
                      font-size: 32px;
                      display: block;
                      margin-bottom: 12px;
                    }

                    .body {
                      padding: 32px;
                    }

                    .greeting {
                      font-family: 'Press Start 2P', monospace;
                      font-size: 11px;
                      color: #5c3d2e;
                      margin-bottom: 20px;
                      line-height: 2;
                    }

                    .message {
                      font-size: 20px;
                      color: #3d2b1a;
                      line-height: 1.6;
                      margin-bottom: 28px;
                    }

                    .token-label {
                      font-family: 'Press Start 2P', monospace;
                      font-size: 9px;
                      color: #5c3d2e;
                      text-transform: uppercase;
                      letter-spacing: 2px;
                      margin-bottom: 10px;
                    }

                    .token-box {
                      background-color: #2a1a0e;
                      border: 2px solid #5c3d2e;
                      box-shadow: 4px 4px 0 #3d2b1a;
                      border-radius: 4px;
                      padding: 18px 20px;
                      margin-bottom: 28px;
                    }

                    .token-box code {
                      font-family: 'VT323', monospace;
                      font-size: 22px;
                      color: #7ecac3;
                      word-break: break-all;
                      letter-spacing: 1px;
                    }

                    .warning-box {
                      background-color: #fff3cd;
                      border: 2px solid #c9973a;
                      box-shadow: 3px 3px 0 #c9973a;
                      border-radius: 4px;
                      padding: 14px 18px;
                      margin-bottom: 28px;
                      font-size: 18px;
                      color: #7a4e12;
                      line-height: 1.5;
                    }

                    .warning-box strong {
                      font-family: 'Press Start 2P', monospace;
                      font-size: 9px;
                      display: block;
                      margin-bottom: 6px;
                      color: #c9973a;
                    }

                    .divider {
                      border: none;
                      border-top: 2px dashed #b09070;
                      margin: 24px 0;
                    }

                    .footer {
                      background-color: #c9a97d;
                      border-top: 3px solid #5c3d2e;
                      padding: 20px 32px;
                      text-align: center;
                    }

                    .footer p {
                      margin: 0;
                      font-size: 18px;
                      color: #5c3d2e;
                      line-height: 1.6;
                    }

                    .footer .brand {
                      font-family: 'Press Start 2P', monospace;
                      font-size: 8px;
                      color: #3d2b1a;
                      margin-top: 10px;
                      display: block;
                      letter-spacing: 1px;
                    }
                  </style>
                </head>
                <body>
                  <div class="wrapper">
                    <div class="header">
                      <span class="pixel-icon">&#128274;</span>
                      <h1>ACCESS TOKEN<br/>READY</h1>
                    </div>

                    <div class="body">
                      <p class="greeting">&gt; HELLO, %s</p>

                      <p class="message">
                        Your access token has been generated.<br/>
                        Use it to authenticate with the Cosy Domain Provider API.
                      </p>

                      <p class="token-label">// YOUR TOKEN</p>
                      <div class="token-box">
                        <code>%s</code>
                      </div>

                      <div class="warning-box">
                        <strong>!! KEEP THIS SAFE !!</strong>
                        Treat this token like a password. Do not share it, commit it to version control,
                        or expose it in client-side code. If compromised, request a new one immediately.
                      </div>

                      <hr class="divider" />

                      <p class="message">
                        If you did not request this token, please contact support straight away.
                      </p>
                    </div>

                    <div class="footer">
                      <p>Happy domain managing! &#127968;</p>
                      <span class="brand">COSY DOMAIN PROVIDER &mdash; &copy; 2026</span>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(username, accessToken);
    }
}
