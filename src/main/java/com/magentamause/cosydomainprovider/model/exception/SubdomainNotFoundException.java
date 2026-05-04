package com.magentamause.cosydomainprovider.model.exception;

import org.springframework.http.HttpStatus;

public class SubdomainNotFoundException extends DomainException {

    public SubdomainNotFoundException(String uuid) {
        super("Subdomain " + uuid + " not found", HttpStatus.NOT_FOUND, "SUBDOMAIN_NOT_FOUND");
    }
}
