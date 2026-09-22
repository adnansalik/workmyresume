package io.github.adnansaliq.workmyresume.service;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class RazorpayClientTest {

    private static final String KEY_ID = "rzp_test_key";
    private static final String KEY_SECRET = "key_secret_for_tests";
    private static final String WEBHOOK_SECRET = "whsec_for_tests";

    private final RazorpayClient client = new RazorpayClient(
            KEY_ID, KEY_SECRET, WEBHOOK_SECRET,
            "https://api.razorpay.com/v1", new ObjectMapper());

    private static String hmacSha256Base64(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void acceptsValidPaymentSignature() throws Exception {
        String order = "order_test_1";
        String payment = "pay_test_1";
        String signature = hmacSha256Base64(order + "|" + payment, KEY_SECRET);

        assertThat(client.verifyPaymentSignature(order, payment, signature)).isTrue();
    }

    @Test
    void rejectsTamperedPaymentSignature() throws Exception {
        String order = "order_test_2";
        String payment = "pay_test_2";
        String signature = hmacSha256Base64("order_test_9|" + payment, KEY_SECRET);

        assertThat(client.verifyPaymentSignature(order, payment, signature)).isFalse();
    }

    @Test
    void rejectsMalformedPaymentSignature() {
        assertThat(client.verifyPaymentSignature("order_test_3", "pay_test_3", "not-base64!!"))
                .isFalse();
        assertThat(client.verifyPaymentSignature("order_test_3", "pay_test_3", null))
                .isFalse();
    }

    @Test
    void acceptsValidWebhookSignature() throws Exception {
        String body = "{\"event\":\"payment.captured\"}";
        String signature = hmacSha256Base64(body, WEBHOOK_SECRET);

        assertThat(client.verifyWebhookSignature(body, signature)).isTrue();
    }

    @Test
    void rejectsWebhookSignedWithWrongSecret() throws Exception {
        String body = "{\"event\":\"payment.captured\"}";
        String signature = hmacSha256Base64(body, "some_other_secret");

        assertThat(client.verifyWebhookSignature(body, signature)).isFalse();
    }
}