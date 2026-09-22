package io.github.adnansaliq.workmyresume.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    @Test
    void blocksRequestsBeyondLimit() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(2, 60, "/auth/");

        assertStatus(filter, newRequest("/auth/login"), 200);
        assertStatus(filter, newRequest("/auth/login"), 200);

        MockHttpServletResponse third = new MockHttpServletResponse();
        filter.doFilter(newRequest("/auth/login"), third, new MockFilterChain());
        assertThat(third.getStatus()).isEqualTo(429);
    }

    @Test
    void unlistedPathIsNotLimited() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1, 60, "/auth/");

        for (int i = 0; i < 5; i++) {
            assertStatus(filter, newRequest("/resumes"), 200);
        }
    }

    @Test
    void windowRolloverResetsTheLimit() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1, 1, "/auth/");

        assertStatus(filter, newRequest("/auth/login"), 200);
        assertStatus(filter, newRequest("/auth/refresh"), 429);

        Thread.sleep(1100);

        assertStatus(filter, newRequest("/auth/login"), 200);
    }

    private void assertStatus(RateLimitFilter filter, MockHttpServletRequest request,
                              int expected) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        assertThat(response.getStatus()).isEqualTo(expected);
    }

    private MockHttpServletRequest newRequest(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(path);
        request.setRemoteAddr("127.0.0.1");
        return request;
    }
}