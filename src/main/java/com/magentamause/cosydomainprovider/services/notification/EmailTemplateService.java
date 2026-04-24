package com.magentamause.cosydomainprovider.services.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private static final String DASH_SPAN =
            "<span style=\"user-select:none;-webkit-user-select:none;\">-</span>";

    private final TemplateEngine templateEngine;

    public String renderVerificationEmail(String username, String accessToken, String verifyLink) {
        Context ctx = new Context();
        ctx.setVariable("username", username);
        ctx.setVariable("accessToken", formatTokenForDisplay(accessToken));
        ctx.setVariable("verifyLink", verifyLink);
        return templateEngine.process("mail/verification-email", ctx);
    }

    public String renderPasswordResetEmail(String username, String resetLink) {
        Context ctx = new Context();
        ctx.setVariable("username", username);
        ctx.setVariable("resetLink", resetLink);
        return templateEngine.process("mail/password-reset-email", ctx);
    }

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
}
