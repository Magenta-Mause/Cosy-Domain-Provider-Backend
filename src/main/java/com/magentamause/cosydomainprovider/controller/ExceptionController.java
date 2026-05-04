package com.magentamause.cosydomainprovider.controller;

import com.magentamause.cosydomainprovider.model.exception.ApiError;
import com.magentamause.cosydomainprovider.model.exception.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class ExceptionController {
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiError> handleDomainException(
            DomainException e, HttpServletRequest request) {
        return ResponseEntity.status(e.getStatus())
                .body(
                        ApiError.builder()
                                .message(e.getMessage())
                                .statusCode(e.getStatus().value())
                                .errorCode(e.getErrorCode())
                                .path(request.getRequestURI())
                                .timestamp(new java.util.Date())
                                .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleException(Exception e, HttpServletRequest request) {
        log.warn("Internal server error: {}", e.getMessage(), e);
        return ResponseEntity.internalServerError()
                .body(
                        ApiError.builder()
                                .message("An unexpected error occurred")
                                .statusCode(500)
                                .errorCode("INTERNAL_SERVER_ERROR")
                                .path(request.getRequestURI())
                                .timestamp(new java.util.Date())
                                .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(
                        ApiError.builder()
                                .message(
                                        "Validation failed: "
                                                + e.getBindingResult().getAllErrors().stream()
                                                        .map(err -> err.getDefaultMessage())
                                                        .reduce((a, b) -> a + "; " + b)
                                                        .orElse(""))
                                .statusCode(400)
                                .errorCode("VALIDATION_ERROR")
                                .path(request.getRequestURI())
                                .timestamp(new java.util.Date())
                                .build());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(
            DataIntegrityViolationException e, HttpServletRequest request) {
        log.warn("Data integrity violation: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(
                        ApiError.builder()
                                .message(
                                        "Resource conflict: a record with the same unique value already exists")
                                .statusCode(409)
                                .errorCode("CONFLICT")
                                .path(request.getRequestURI())
                                .timestamp(new java.util.Date())
                                .build());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatusException(
            ResponseStatusException e, HttpServletRequest request) {
        return ResponseEntity.status(e.getStatusCode())
                .body(
                        ApiError.builder()
                                .message(e.getReason() != null ? e.getReason() : e.getMessage())
                                .statusCode(e.getStatusCode().value())
                                .errorCode(e.getStatusCode().toString())
                                .path(request.getRequestURI())
                                .timestamp(new java.util.Date())
                                .build());
    }

    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<ApiError> handleMissingCookie(
            MissingRequestCookieException e, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(
                        ApiError.builder()
                                .message("Required cookie is missing: " + e.getCookieName())
                                .statusCode(401)
                                .errorCode("UNAUTHORIZED")
                                .path(request.getRequestURI())
                                .timestamp(new java.util.Date())
                                .build());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFoundException(
            NoResourceFoundException e, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(
                        ApiError.builder()
                                .message("Resource not found")
                                .statusCode(404)
                                .errorCode("RESOURCE_NOT_FOUND")
                                .path(request.getRequestURI())
                                .timestamp(new java.util.Date())
                                .build());
    }
}
