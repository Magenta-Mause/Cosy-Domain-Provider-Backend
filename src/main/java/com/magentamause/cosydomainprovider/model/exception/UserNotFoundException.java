package com.magentamause.cosydomainprovider.model.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends DomainException {

    private UserNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "USER_NOT_FOUND");
    }

    public static UserNotFoundException byId(String uuid) {
        return new UserNotFoundException("User with id " + uuid + " not found");
    }

    public static UserNotFoundException byEmail(String email) {
        return new UserNotFoundException("User with email " + email + " not found");
    }
}
