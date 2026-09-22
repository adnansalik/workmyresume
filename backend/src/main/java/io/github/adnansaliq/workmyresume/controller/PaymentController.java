package io.github.adnansaliq.workmyresume.controller;

import io.github.adnansaliq.workmyresume.dto.CheckoutResponse;
import io.github.adnansaliq.workmyresume.dto.VerifyPaymentRequest;
import io.github.adnansaliq.workmyresume.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/payments/checkout")
    public ResponseEntity<CheckoutResponse> createCheckout(Authentication authentication) {
        CheckoutResponse response = paymentService.createCheckout(authentication.getName());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/payments/verify")
    public ResponseEntity<Map<String, Boolean>> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request,
            Authentication authentication) {

        paymentService.verifyAndActivate(authentication.getName(), request);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /** Called by Razorpay; authenticated by the X-Razorpay-Signature header. */
    @PostMapping("/payments/webhook")
    public ResponseEntity<Map<String, String>> webhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        paymentService.handleWebhook(rawBody, signature);
        return ResponseEntity.ok(Map.of("received", "true"));
    }
}