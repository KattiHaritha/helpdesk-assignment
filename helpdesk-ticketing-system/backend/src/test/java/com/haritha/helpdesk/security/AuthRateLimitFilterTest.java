package com.haritha.helpdesk.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthRateLimitFilterTest {
    @Test
    void loginIsLimitedAfterTenAttemptsFromSameAddress() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter();
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
            request.setRemoteAddr("192.0.2.10");
            filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        }

        MockHttpServletRequest limitedRequest = new MockHttpServletRequest("POST", "/api/auth/login");
        limitedRequest.setRemoteAddr("192.0.2.10");
        MockHttpServletResponse limitedResponse = new MockHttpServletResponse();
        filter.doFilter(limitedRequest, limitedResponse, new MockFilterChain());

        assertEquals(429, limitedResponse.getStatus());
        assertEquals("60", limitedResponse.getHeader("Retry-After"));
    }

    @Test
    void otherAddressesHaveSeparateLimits() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter();
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
            request.setRemoteAddr("192.0.2.10");
            filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        }

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("192.0.2.11");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
    }
}
