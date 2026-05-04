package com.magentamause.cosydomainprovider.model.exception;

import org.springframework.http.HttpStatus;

public class LabelConflictException extends DomainException {

    private LabelConflictException(String message) {
        super(message, HttpStatus.CONFLICT, "LABEL_CONFLICT");
    }

    public static LabelConflictException reserved(String label) {
        return new LabelConflictException("Label '" + label + "' is reserved");
    }

    public static LabelConflictException taken(String label) {
        return new LabelConflictException("Label '" + label + "' is already taken");
    }
}
