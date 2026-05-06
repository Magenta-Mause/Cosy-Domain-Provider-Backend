package com.magentamause.cosydomainprovider.security.stagingauth;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.magentamause.cosydomainprovider.configuration.stagingauth.StagingAuthProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class StagingAuthFilterTest {

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "secret123";

    private StagingAuthFilter filter;

    @BeforeEach
    void setUp() {
        StagingAuthProperties props = new StagingAuthProperties(true, USERNAME, PASSWORD);
        filter = new StagingAuthFilter(props);
    }

    private String computeExpectedCookieValue() throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(PASSWORD.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hmac = mac.doFinal(USERNAME.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hmac);
    }

    private String basicHeader(String user, String pass) {
        String raw = user + ":" + pass;
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void disabled_passesThrough() throws ServletException, IOException {
        StagingAuthProperties disabled = new StagingAuthProperties(false, USERNAME, PASSWORD);
        StagingAuthFilter disabledFilter = new StagingAuthFilter(disabled);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        disabledFilter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void actuatorPath_passesThrough() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void stripeEventsPath_passesThrough() throws ServletException, IOException {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/v1/stripe-events");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void noCredentials_returns401() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/domains");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void validStagingCookie_passesThrough() throws Exception {
        String cookieValue = computeExpectedCookieValue();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/domains");
        request.setCookies(new Cookie(StagingAuthFilter.COOKIE_NAME, cookieValue));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void invalidStagingCookie_returns401() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/domains");
        request.setCookies(new Cookie(StagingAuthFilter.COOKIE_NAME, "wrong-value"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void authEndpoint_GET_validCookie_returns200() throws Exception {
        String cookieValue = computeExpectedCookieValue();
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", StagingAuthFilter.AUTH_PATH);
        request.setCookies(new Cookie(StagingAuthFilter.COOKIE_NAME, cookieValue));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void authEndpoint_GET_noCookie_returns401() throws ServletException, IOException {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", StagingAuthFilter.AUTH_PATH);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void authEndpoint_POST_validCredentials_setsCookieAndReturns200()
            throws ServletException, IOException {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", StagingAuthFilter.AUTH_PATH);
        request.addHeader("Authorization", basicHeader(USERNAME, PASSWORD));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getCookies())
                .anyMatch(c -> StagingAuthFilter.COOKIE_NAME.equals(c.getName()));
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void authEndpoint_POST_wrongPassword_returns401() throws ServletException, IOException {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", StagingAuthFilter.AUTH_PATH);
        request.addHeader("Authorization", basicHeader(USERNAME, "wrongpassword"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void authEndpoint_POST_noAuthHeader_returns401() throws ServletException, IOException {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", StagingAuthFilter.AUTH_PATH);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void authEndpoint_PUT_returns405() throws ServletException, IOException {
        MockHttpServletRequest request =
                new MockHttpServletRequest("PUT", StagingAuthFilter.AUTH_PATH);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(405);
    }

    @Test
    void authEndpoint_POST_invalidBase64_returns401() throws ServletException, IOException {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", StagingAuthFilter.AUTH_PATH);
        request.addHeader("Authorization", "Basic not-valid-base64!!!");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void authEndpoint_POST_noColonInCredentials_returns401() throws ServletException, IOException {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", StagingAuthFilter.AUTH_PATH);
        String encoded =
                Base64.getEncoder().encodeToString("nocolon".getBytes(StandardCharsets.UTF_8));
        request.addHeader("Authorization", "Basic " + encoded);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
    }
}
