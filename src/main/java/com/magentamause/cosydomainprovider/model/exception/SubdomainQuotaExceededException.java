package com.magentamause.cosydomainprovider.model.exception;

import org.springframework.http.HttpStatus;

public class SubdomainQuotaExceededException extends DomainException {

    public SubdomainQuotaExceededException(int limit) {
        super(
                "Subdomain quota reached (" + limit + " per user)",
                HttpStatus.FORBIDDEN,
                "SUBDOMAIN_QUOTA_EXCEEDED");
    }
}
