package com.magentamause.cosydomainprovider.controller.v1.implementation;

import com.magentamause.cosydomainprovider.controller.v1.schema.BillingApi;
import com.magentamause.cosydomainprovider.model.core.BillingPortalResponseDto;
import com.magentamause.cosydomainprovider.services.auth.SecurityContextService;
import com.magentamause.cosydomainprovider.services.billing.StripeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
public class BillingController implements BillingApi {

    private final StripeService stripeService;
    private final SecurityContextService securityContextService;

    @Override
    public ResponseEntity<BillingPortalResponseDto> getBillingPortalUrl() {
        String url = stripeService.createBillingPortalSession(securityContextService.getUser());
        return ResponseEntity.ok(new BillingPortalResponseDto(url));
    }

    @Override
    public ResponseEntity<BillingPortalResponseDto> getCheckoutUrl() {
        var user = securityContextService.getUser();
        if (!user.isVerified()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Email not verified");
        }
        String url = stripeService.createCheckoutSession(user);
        return ResponseEntity.ok(new BillingPortalResponseDto(url));
    }
}
