package com.magentamause.cosydomainprovider.controller.v1.implementation;

import com.magentamause.cosydomainprovider.controller.v1.schema.StripeWebhookApi;
import com.magentamause.cosydomainprovider.services.billing.StripeService;
import com.stripe.exception.SignatureVerificationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class StripeWebhookController implements StripeWebhookApi {

    private final StripeService stripeService;

    @Override
    public ResponseEntity<String> consumeEvent(String body, String sigHeader) {
        try {
            stripeService.handleWebhookEvent(body, sigHeader);
            return ResponseEntity.ok("OK");
        } catch (SignatureVerificationException e) {
            log.warn("Stripe webhook signature verification failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Bad signature");
        }
    }
}
