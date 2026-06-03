package com.airline.payment.controller;

import com.airline.common.ApiResponse;
import com.airline.payment.dto.CreatePaymentRequest;
import com.airline.payment.dto.PaymentDTO;
import com.airline.payment.service.PaymentService;
import com.airline.payment.service.StripeService;
import com.airline.user.entity.User;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final StripeService stripeService;

    // =============================================
    // POST /api/payments/create-intent
    // Authenticated — creates a Stripe PaymentIntent
    // =============================================

    @PostMapping("/create-intent")
    public ResponseEntity<ApiResponse<PaymentDTO>> createPaymentIntent(
            @Valid @RequestBody CreatePaymentRequest request,
            Authentication authentication) {

        Long userId = ((User) authentication.getPrincipal()).getId();
        PaymentDTO payment = paymentService.createPayment(request, userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(payment, "PaymentIntent created — use clientSecret to confirm"));
    }

    // =============================================
    // POST /api/payments/webhook
    // Public — Stripe webhook (verified by signature, NOT by JWT)
    // =============================================

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(HttpServletRequest request) {

        // 1. Read raw body — Stripe signature verification needs unmodified payload
        String payload;
        try {
            payload = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to read webhook payload: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to read request body");
        }

        // 2. Get Stripe signature header
        String sigHeader = request.getHeader("Stripe-Signature");
        if (sigHeader == null || sigHeader.isBlank()) {
            log.warn("Missing Stripe-Signature header");
            return ResponseEntity.badRequest().body("Missing Stripe-Signature header");
        }

        // 3. Verify signature and parse event
        Event event;
        try {
            event = stripeService.verifyWebhookSignature(payload, sigHeader);
        } catch (Exception e) {
            log.error("Webhook signature verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        // 4. Extract PaymentIntent ID from the event
        Optional<StripeObject> stripeObject = event.getDataObjectDeserializer().getObject();
        if (stripeObject.isEmpty()) {
            log.warn("Could not deserialize webhook event data for event: {}", event.getId());
            return ResponseEntity.ok("Event received but could not deserialize");
        }

        // 5. Dispatch based on event type
        String eventType = event.getType();
        log.info("Stripe webhook received: type={}, eventId={}", eventType, event.getId());

        switch (eventType) {
            case "payment_intent.succeeded" -> {
                PaymentIntent intent = (PaymentIntent) stripeObject.get();
                paymentService.handlePaymentSuccess(intent.getId());
                log.info("Processed payment_intent.succeeded: {}", intent.getId());
            }
            case "payment_intent.payment_failed" -> {
                PaymentIntent intent = (PaymentIntent) stripeObject.get();
                paymentService.handlePaymentFailure(intent.getId());
                log.info("Processed payment_intent.payment_failed: {}", intent.getId());
            }
            default -> {
                log.info("Unhandled webhook event type: {}", eventType);
            }
        }

        // 6. Always return 200 to Stripe — prevents unnecessary retries
        return ResponseEntity.ok("Webhook processed");
    }
}
