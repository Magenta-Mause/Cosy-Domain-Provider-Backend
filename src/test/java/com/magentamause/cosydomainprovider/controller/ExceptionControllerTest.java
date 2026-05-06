package com.magentamause.cosydomainprovider.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.magentamause.cosydomainprovider.model.exception.ApiError;
import com.magentamause.cosydomainprovider.model.exception.LabelConflictException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class ExceptionControllerTest {

    private ExceptionController controller;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        controller = new ExceptionController();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/test");
    }

    @Test
    void handleDomainException_returnsCorrectStatus() {
        LabelConflictException ex = LabelConflictException.reserved("www");
        ResponseEntity<ApiError> resp = controller.handleDomainException(ex, request);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody().getErrorCode()).isEqualTo("LABEL_CONFLICT");
        assertThat(resp.getBody().getPath()).isEqualTo("/api/v1/test");
    }

    @Test
    void handleException_returns500() {
        Exception ex = new RuntimeException("unexpected");
        ResponseEntity<ApiError> resp = controller.handleException(ex, request);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resp.getBody().getErrorCode()).isEqualTo("INTERNAL_SERVER_ERROR");
    }

    @Test
    void handleValidationException_returns400() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult br = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(br);
        FieldError fieldError = new FieldError("obj", "field", "must not be blank");
        when(br.getAllErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ApiError> resp = controller.handleValidationException(ex, request);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getErrorCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(resp.getBody().getMessage()).contains("must not be blank");
    }

    @Test
    void handleDataIntegrityViolation_returns409() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("constraint");
        ResponseEntity<ApiError> resp = controller.handleDataIntegrityViolation(ex, request);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody().getErrorCode()).isEqualTo("CONFLICT");
    }

    @Test
    void handleResponseStatusException_returnsCorrectStatus() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "not here");
        ResponseEntity<ApiError> resp = controller.handleResponseStatusException(ex, request);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody().getMessage()).isEqualTo("not here");
    }

    @Test
    void handleMissingCookie_returns401() {
        MissingRequestCookieException ex = new MissingRequestCookieException("refreshToken", null);
        ResponseEntity<ApiError> resp = controller.handleMissingCookie(ex, request);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resp.getBody().getErrorCode()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    void handleNoResourceFound_returns404() {
        NoResourceFoundException ex =
                new NoResourceFoundException(HttpMethod.GET, "/missing", "not found");
        ResponseEntity<ApiError> resp = controller.handleResourceNotFoundException(ex, request);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody().getErrorCode()).isEqualTo("RESOURCE_NOT_FOUND");
    }
}
