package io.github.adnansaliq.workmyresume;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Without Razorpay credentials the checkout endpoint must fail cleanly with 503
 * instead of making a doomed API call.
 */
class PaymentNotConfiguredTest extends BaseIntegrationTest {

    @Test
    void checkoutWithoutCredentialsReturns503() throws Exception {
        String token = login(register("pay-unconfigured@example.com").getEmail());

        mockMvc.perform(post("/payments/checkout")
                        .header("Authorization", BEARER + token))
                .andExpect(status().isServiceUnavailable());
    }
}