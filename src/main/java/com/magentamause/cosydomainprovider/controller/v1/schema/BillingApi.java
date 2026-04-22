package com.magentamause.cosydomainprovider.controller.v1.schema;

import com.magentamause.cosydomainprovider.model.core.BillingPortalResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/v1/billing")
public interface BillingApi {

    @GetMapping("/portal")
    ResponseEntity<BillingPortalResponseDto> getBillingPortalUrl();
}
