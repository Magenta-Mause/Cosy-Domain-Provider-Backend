package com.magentamause.cosydomainprovider.model.exception;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class ApiException {
    private final String message;
    private final int statusCode;
    private final String errorCode;
    private final String path;
    private final Date timestamp;
}
