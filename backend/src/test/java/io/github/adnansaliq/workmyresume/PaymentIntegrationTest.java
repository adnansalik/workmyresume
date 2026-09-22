package io.github.adnansaliq.workmyresume;

import io.github.adnansaliq.workmyresume.domain.Payment;
import io.github.adnansaliq.workmyresume.domain.PaymentStatus;
import io.github.adnansaliq.workmyresume.domain.Plan;
import io.github.adnansaliq.workmyresume.domain.SubscriptionStatus;
import io.github.adnansaliq.workmyresume.domain.User;
import io.github.adnansaliq.workmyresume.repository.PaymentRepository;
import io.github.adnansaliq.workmyresume.repository.SubscriptionRepository;
import io.github.adnansaliq.workmyresume.service.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 7: Razorpay verify + webhook flows activate the PRO subscription.
 * Payment credentials are injected so signatures verify; checkout itself is
 * covered in PaymentNotConfiguredTest (no keys wired in the base context).
 */
class PaymentIntegrationTest extends BaseIntegrationTest {

    private static final String KEY_SECRET = "key_secret_for_tests";
    private static final String WEBHOOK_SECRET = "whsec_for_tests";

    @DynamicPropertySource
    static void paymentCredentials(DynamicPropertyRegistry registry) {
        registry.add("app.payments.razorpay.key-id", () -> "rzp_test_key");
        registry.add("app.payments.razorpay.key-secret", () -> KEY_SECRET);
        registry.add("app.payments.razorpay.webhook-secret", () -> WEBHOOK_SECRET);
    }

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    SubscriptionRepository subscriptionRepository;

    @Autowired
    SubscriptionService subscriptionService;

    @Test
    void verifiedCheckoutActivatesPro() throws Exception {
        User user = register("pay-verify@example.com");
        String token = login(user.getEmail());
        String orderId = "order_verify_1";
        String paymentId = "pay_verify_1";
        savePayment(user.getId(), orderId);

        String signature = hmacSha256Base64(orderId + "|" + paymentId, KEY_SECRET);

        mockMvc.perform(post("/payments/verify")
                        .header("Authorization", BEARER + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"%s","paymentId":"%s","signature":"%s"}
                                """.formatted(orderId, paymentId, signature)))
                .andExpect(status().isOk());

        User reloaded = userRepository.findByEmail(user.getEmail()).orElseThrow();
        assertThat(reloaded.getPlan()).isEqualTo(Plan.PRO);
        assertThat(subscriptionService.effectivePlan(reloaded)).isEqualTo(Plan.PRO);
        assertThat(paymentRepository.findByRazorpayOrderId(orderId).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.VERIFIED);
    }

    @Test
    void invalidPaymentSignatureIsRejected() throws Exception {
        User user = register("pay-badsig@example.com");
        String token = login(user.getEmail());
        String orderId = "order_badsig_1";
        savePayment(user.getId(), orderId);

        mockMvc.perform(post("/payments/verify")
                        .header("Authorization", BEARER + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"%s","paymentId":"pay_1","signature":"not-a-signature"}
                                """.formatted(orderId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void webhookActivatesProAndIsIdempotent() throws Exception {
        User user = register("pay-webhook@example.com");
        String orderId = "order_wh_1";
        savePayment(user.getId(), orderId);

        String body = """
                {"entity":"event","event":"payment.captured",
                 "payload":{"payment":{"entity":{"id":"pay_wh_1","order_id":"%s"}}}}
                """.formatted(orderId);
        String signature = hmacSha256Base64(body, WEBHOOK_SECRET);

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/payments/webhook")
                            .header("X-Razorpay-Signature", signature)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }

        User reloaded = userRepository.findByEmail(user.getEmail()).orElseThrow();
        assertThat(reloaded.getPlan()).isEqualTo(Plan.PRO);
        // Idempotency: exactly one ACTIVE subscription despite two webhooks.
        assertThat(subscriptionRepository.findByUserIdAndStatus(
                        reloaded.getId(), SubscriptionStatus.ACTIVE)).hasSize(1);
    }

    @Test
    void webhookWithBadSignatureIsRejected() throws Exception {
        mockMvc.perform(post("/payments/webhook")
                        .header("X-Razorpay-Signature", "bad-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\":\"payment.captured\",\"payload\":{}}"))
                .andExpect(status().isBadRequest());
    }

    private void savePayment(Long userId, String orderId) {
        User user = userRepository.findById(userId).orElseThrow();
        Payment payment = new Payment();
        payment.setUser(user);
        payment.setRazorpayOrderId(orderId);
        payment.setAmount(Plan.PRO.getAmountPaise());
        payment.setCurrency("INR");
        payment.setStatus(PaymentStatus.CREATED);
        paymentRepository.save(payment);
    }

    private static String hmacSha256Base64(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }
}