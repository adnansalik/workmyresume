package io.github.adnansaliq.workmyresume.service;

import io.github.adnansaliq.workmyresume.domain.Payment;
import io.github.adnansaliq.workmyresume.domain.PaymentStatus;
import io.github.adnansaliq.workmyresume.domain.Plan;
import io.github.adnansaliq.workmyresume.domain.User;
import io.github.adnansaliq.workmyresume.dto.CheckoutResponse;
import io.github.adnansaliq.workmyresume.dto.VerifyPaymentRequest;
import io.github.adnansaliq.workmyresume.exception.BadRequestException;
import io.github.adnansaliq.workmyresume.exception.PaymentNotConfiguredException;
import io.github.adnansaliq.workmyresume.exception.ResourceNotFoundException;
import io.github.adnansaliq.workmyresume.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.Set;

/**
 * Orchestrates the payment lifecycle: create a Razorpay order, verify the
 * client-side payment signature, and consume webhooks. Activation is idempotent
 * (a CREATED payment is activated exactly once).
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final String CURRENCY = "INR";
    private static final Set<String> ACTIVATION_EVENTS =
            Set.of("payment.captured", "order.paid");

    private final RazorpayClient razorpayClient;
    private final PaymentRepository paymentRepository;
    private final SubscriptionService subscriptionService;
    private final ResumeAccessService resumeAccessService;
    private final ObjectMapper objectMapper;

    public PaymentService(RazorpayClient razorpayClient,
                          PaymentRepository paymentRepository,
                          SubscriptionService subscriptionService,
                          ResumeAccessService resumeAccessService,
                          ObjectMapper objectMapper) {
        this.razorpayClient = razorpayClient;
        this.paymentRepository = paymentRepository;
        this.subscriptionService = subscriptionService;
        this.resumeAccessService = resumeAccessService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CheckoutResponse createCheckout(String email) {
        requirePaymentsConfigured();

        User user = resumeAccessService.requireUser(email);
        long amount = Plan.PRO.getAmountPaise();
        String orderId = razorpayClient.createOrder(user.getEmail(), amount);

        Payment payment = new Payment();
        payment.setUser(user);
        payment.setRazorpayOrderId(orderId);
        payment.setAmount(amount);
        payment.setCurrency(CURRENCY);
        payment.setStatus(PaymentStatus.CREATED);
        paymentRepository.save(payment);

        CheckoutResponse response = new CheckoutResponse();
        response.setOrderId(orderId);
        response.setKeyId(razorpayClient.keyId());
        response.setCurrency(CURRENCY);
        response.setAmount(amount);
        return response;
    }

    /** Verifies the client-side signature and activates PRO exactly once. */
    @Transactional
    public void verifyAndActivate(String email, VerifyPaymentRequest request) {
        requirePaymentsConfigured();

        User user = resumeAccessService.requireUser(email);
        Payment payment = paymentRepository.findByRazorpayOrderId(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        if (!payment.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Payment not found");
        }
        if (!razorpayClient.verifyPaymentSignature(
                request.getOrderId(), request.getPaymentId(), request.getSignature())) {
            throw new BadRequestException("Invalid payment signature");
        }

        activateIfNotVerified(payment);
    }

    /** Consumes a Razorpay webhook after verifying its signature. */
    @Transactional
    public void handleWebhook(String rawBody, String signature) {
        if (!razorpayClient.webhookConfigured()) {
            throw new BadRequestException("Webhooks not configured");
        }
        if (!razorpayClient.verifyWebhookSignature(rawBody, signature)) {
            throw new BadRequestException("Invalid webhook signature");
        }

        try {
            JsonNode root = objectMapper.readTree(rawBody);
            String event = root.path("event").asText();
            if (!ACTIVATION_EVENTS.contains(event)) {
                return;
            }

            String orderId = root.path("payload").path("payment").path("entity")
                    .path("order_id").asText();
            if (orderId.isBlank()) {
                orderId = root.path("payload").path("order").path("entity").path("id").asText();
            }
            if (orderId.isBlank()) {
                return;
            }

            Payment payment = paymentRepository.findByRazorpayOrderId(orderId).orElse(null);
            if (payment == null) {
                log.warn("Webhook {} references unknown order {}", event, orderId);
                return;
            }
            activateIfNotVerified(payment);
        } catch (Exception e) {
            throw new BadRequestException("Malformed webhook payload");
        }
    }

    private void activateIfNotVerified(Payment payment) {
        if (payment.getStatus() == PaymentStatus.VERIFIED) {
            return; // idempotent: already processed
        }
        subscriptionService.activate(payment.getUser().getEmail(), Plan.PRO);
        payment.setStatus(PaymentStatus.VERIFIED);
        payment.setVerifiedAt(OffsetDateTime.now());
        paymentRepository.save(payment);
    }

    private void requirePaymentsConfigured() {
        if (!razorpayClient.isConfigured()) {
            throw new PaymentNotConfiguredException(
                    "Payments are not configured - set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET");
        }
    }
}