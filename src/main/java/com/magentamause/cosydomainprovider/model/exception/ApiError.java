package com.magentamause.cosydomainprovider.model.exception;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiError {
    private final String message;
    private final int statusCode;
    private final String errorCode;
    private final String path;
    private final Instant timestamp;
}
