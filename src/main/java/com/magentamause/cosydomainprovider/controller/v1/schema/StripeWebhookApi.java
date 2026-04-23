package com.magentamause.cosydomainprovider.controller.v1.schema;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/v1/stripe-events")
public interface StripeWebhookApi {

    @PostMapping
    ResponseEntity<String> consumeEvent(
            @RequestBody String body,
            @RequestHeader("Stripe-Signature") String sigHeader);
}
