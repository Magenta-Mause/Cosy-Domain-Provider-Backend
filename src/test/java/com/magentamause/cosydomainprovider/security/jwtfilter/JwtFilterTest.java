package com.magentamause.cosydomainprovider.security.jwtfilter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.model.exception.UserNotFoundException;
import com.magentamause.cosydomainprovider.services.core.UserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock private JwtUtils jwtUtils;
    @Mock private UserService userService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    private JwtFilter jwtFilter;

    @BeforeEach
    void setUp() {
        jwtFilter = new JwtFilter(jwtUtils, userService);
        SecurityContextHolder.clearContext();
    }

    @Test
    void noToken_passesThrough() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
        verify(jwtUtils, never()).getTokenContentBody(any(), any());
    }

    @Test
    void blankToken_passesThrough() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer ");
        jwtFilter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void validBearerToken_setsAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("u1");
        when(jwtUtils.getTokenContentBody("valid-token", JwtTokenBody.TokenType.IDENTITY_TOKEN))
                .thenReturn(claims);
        UserEntity user =
                UserEntity.builder().uuid("u1").username("alice").email("a@a.com").build();
        when(userService.getUserByUuid("u1")).thenReturn(user);

        jwtFilter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void invalidToken_sends401() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer bad-token");
        when(jwtUtils.getTokenContentBody("bad-token", JwtTokenBody.TokenType.IDENTITY_TOKEN))
                .thenThrow(new SecurityException("invalid"));

        jwtFilter.doFilterInternal(request, response, filterChain);
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed");
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void validTokenButUserNotFound_sends401() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("missing");
        when(jwtUtils.getTokenContentBody("valid-token", JwtTokenBody.TokenType.IDENTITY_TOKEN))
                .thenReturn(claims);
        when(userService.getUserByUuid("missing"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        jwtFilter.doFilterInternal(request, response, filterChain);
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed");
    }

    @Test
    void tokenFromQueryParam_isIgnored() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
        verify(request, never()).getParameter("authToken");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void validTokenButUserDeleted_sends401() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("gone");
        when(jwtUtils.getTokenContentBody("valid-token", JwtTokenBody.TokenType.IDENTITY_TOKEN))
                .thenReturn(claims);
        when(userService.getUserByUuid("gone")).thenThrow(UserNotFoundException.byId("gone"));

        jwtFilter.doFilterInternal(request, response, filterChain);
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed");
        verify(filterChain, never()).doFilter(any(), any());
    }
}
