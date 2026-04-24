package com.magentamause.cosydomainprovider.controller.v1.schema;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(
        name = "Stripe Webhook",
        description = "Stripe event webhook consumer (internal — not for client use)")
@RequestMapping("/v1/stripe-events")
public interface StripeWebhookApi {

    @Operation(
            summary = "Consume a Stripe webhook event",
            description = "Verifies the Stripe-Signature header and processes the event.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Event processed"),
        @ApiResponse(responseCode = "400", description = "Invalid Stripe signature")
    })
    @PostMapping
    ResponseEntity<String> consumeEvent(
            @RequestBody String body,
            @Parameter(description = "Stripe webhook signature header")
                    @RequestHeader("Stripe-Signature")
                    String sigHeader);
}
