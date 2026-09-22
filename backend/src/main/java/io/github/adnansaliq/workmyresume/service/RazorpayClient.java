package io.github.adnansaliq.workmyresume.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/**
 * Minimal Razorpay integration: order creation plus signature verification for
 * payments and webhooks. Configured via app.payments.razorpay.* (env: RAZORPAY_*).
 */
@Service
public class RazorpayClient {

    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;

    public RazorpayClient(@Value("${app.payments.razorpay.key-id:}") String keyId,
                          @Value("${app.payments.razorpay.key-secret:}") String keySecret,
                          @Value("${app.payments.razorpay.webhook-secret:}") String webhookSecret,
                          @Value("${app.payments.razorpay.base-url:https://api.razorpay.com/v1}")
                          String baseUrl,
                          ObjectMapper objectMapper) {
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(30));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    public String keyId() {
        return keyId;
    }

    public boolean isConfigured() {
        return keyId != null && !keyId.isBlank() && keySecret != null && !keySecret.isBlank();
    }

    public boolean webhookConfigured() {
        return webhookSecret != null && !webhookSecret.isBlank();
    }

    /** Creates a Razorpay order and returns its id (e.g. "order_xxxx"). */
    public String createOrder(String email, long amountPaise) {
        Map<String, Object> body = Map.of(
                "amount", amountPaise,
                "currency", "INR",
                "receipt", "resume-pro",
                "notes", Map.of("email", email));

        String response = restClient.post()
                .uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.setBasicAuth(keyId, keySecret))
                .body(body)
                .exchange((request, res) -> {
                    if (res.getStatusCode().isError()) {
                        throw new IllegalStateException("Razorpay API error: " + res.getStatusCode());
                    }
                    try (InputStream in = res.getBody()) {
                        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    }
                });

        try {
            JsonNode root = objectMapper.readTree(response);
            return root.path("id").asText();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Razorpay order response", e);
        }
    }

    /** Verifies the client-side payment signature: HMAC(orderId|paymentId) with the key secret. */
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        return verifySignature(orderId + "|" + paymentId, signature, keySecret);
    }

    /** Verifies a webhook body signature: HMAC(rawBody) with the webhook secret. */
    public boolean verifyWebhookSignature(String rawBody, String signature) {
        return verifySignature(rawBody, signature, webhookSecret);
    }

    private boolean verifySignature(String data, String signature, String secret) {
        if (signature == null || signature.isBlank()) {
            return false;
        }
        try {
            byte[] expected = hmacSha256(data, secret);
            byte[] provided = Base64.getDecoder().decode(signature);
            return MessageDigest.isEqual(expected, provided);
        } catch (IllegalArgumentException e) {
            return false; // malformed base64 signature
        }
    }

    private byte[] hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC computation failed", e);
        }
    }
}