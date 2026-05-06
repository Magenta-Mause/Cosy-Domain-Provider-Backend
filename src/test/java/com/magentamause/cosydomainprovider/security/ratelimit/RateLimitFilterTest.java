package com.magentamause.cosydomainprovider.security.ratelimit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.magentamause.cosydomainprovider.configuration.ratelimit.RateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        RateLimitProperties.Rule authRule = new RateLimitProperties.Rule(100, 60);
        RateLimitProperties.Rule globalRule = new RateLimitProperties.Rule(200, 60);
        RateLimitProperties props = new RateLimitProperties(authRule, globalRule);
        filter = new RateLimitFilter(props);
    }

    @Test
    void globalPath_passesThrough() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/domains");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void authPath_passesThrough() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void xForwardedFor_usesFirstIp() throws ServletException, IOException {
        MockHttpServletRequest request1 = new MockHttpServletRequest("GET", "/api/v1/test");
        request1.addHeader("X-Forwarded-For", "1.2.3.4, 5.6.7.8");

        MockHttpServletRequest request2 = new MockHttpServletRequest("GET", "/api/v1/test");
        request2.addHeader("X-Forwarded-For", "1.2.3.4");

        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse resp1 = new MockHttpServletResponse();
        MockHttpServletResponse resp2 = new MockHttpServletResponse();

        filter.doFilterInternal(request1, resp1, chain);
        filter.doFilterInternal(request2, resp2, chain);

        verify(chain, times(2)).doFilter(any(), any());
    }

    @Test
    void rateLimitExceeded_returns429() throws ServletException, IOException {
        RateLimitProperties.Rule authRule = new RateLimitProperties.Rule(1, 60);
        RateLimitProperties.Rule globalRule = new RateLimitProperties.Rule(1, 60);
        RateLimitProperties props = new RateLimitProperties(authRule, globalRule);
        RateLimitFilter tightFilter = new RateLimitFilter(props);

        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest req1 = new MockHttpServletRequest("GET", "/api/v1/test");
        req1.setRemoteAddr("9.9.9.9");
        MockHttpServletResponse resp1 = new MockHttpServletResponse();
        tightFilter.doFilterInternal(req1, resp1, chain);

        MockHttpServletRequest req2 = new MockHttpServletRequest("GET", "/api/v1/test");
        req2.setRemoteAddr("9.9.9.9");
        MockHttpServletResponse resp2 = new MockHttpServletResponse();
        tightFilter.doFilterInternal(req2, resp2, chain);

        verify(chain, times(1)).doFilter(any(), any());
        assertThat(resp2.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(resp2.getContentType()).isEqualTo("application/json");
    }

    @Test
    void remoteAddr_usedWhenNoForwardedHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");
        request.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
