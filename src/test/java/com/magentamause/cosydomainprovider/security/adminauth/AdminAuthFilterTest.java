package com.magentamause.cosydomainprovider.security.adminauth;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.magentamause.cosydomainprovider.configuration.admin.AdminProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminAuthFilterTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain chain;

    private AdminProperties properties;
    private AdminAuthFilter filter;
    private StringWriter responseBody;

    @BeforeEach
    void setUp() throws Exception {
        properties = new AdminProperties();
        properties.setSecretKey("secret");
        filter = new AdminAuthFilter(properties);
        responseBody = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));
    }

    @Test
    void validKey_passesThrough() throws Exception {
        when(request.getHeader(AdminAuthFilter.HEADER_NAME)).thenReturn("secret");

        filter.doFilterInternal(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    void invalidKey_returns401() throws Exception {
        when(request.getHeader(AdminAuthFilter.HEADER_NAME)).thenReturn("wrong");

        filter.doFilterInternal(request, response, chain);
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(chain, never()).doFilter(any(), any());
        assertThat(responseBody.toString()).contains("Invalid admin key");
    }

    @Test
    void missingHeader_returns401() throws Exception {
        when(request.getHeader(AdminAuthFilter.HEADER_NAME)).thenReturn(null);

        filter.doFilterInternal(request, response, chain);
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void missingConfiguredKey_returns401() throws Exception {
        properties.setSecretKey(null);
        when(request.getHeader(AdminAuthFilter.HEADER_NAME)).thenReturn("anything");

        filter.doFilterInternal(request, response, chain);
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(chain, never()).doFilter(any(), any());
    }
}
