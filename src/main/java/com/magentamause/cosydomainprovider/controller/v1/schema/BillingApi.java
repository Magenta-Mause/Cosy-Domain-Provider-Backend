package com.magentamause.cosydomainprovider.controller.v1.schema;

import com.magentamause.cosydomainprovider.model.core.BillingPortalResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Billing", description = "Stripe billing portal and subscription checkout")
@RequestMapping("/v1/billing")
public interface BillingApi {

    @Operation(summary = "Get Stripe billing portal URL for the authenticated user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Portal URL returned"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/portal")
    ResponseEntity<BillingPortalResponseDto> getBillingPortalUrl();

    @Operation(
            summary = "Get Stripe checkout URL to upgrade to Plus",
            description = "Requires a verified email address.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Checkout URL returned"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Email not verified")
    })
    @PostMapping("/checkout")
    ResponseEntity<BillingPortalResponseDto> getCheckoutUrl();
}
