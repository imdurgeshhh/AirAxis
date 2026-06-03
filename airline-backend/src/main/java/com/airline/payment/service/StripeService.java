package com.airline.payment.service;

import com.airline.config.AppProperties;
import com.airline.exception.PaymentException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Thin wrapper around the Stripe SDK.
 * Isolates Stripe API calls from business logic for testability.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StripeService {

    private final AppProperties appProperties;

    /**
     * Creates a Stripe PaymentIntent.
     *
     * @param amountInPaise amount in smallest currency unit (paise for INR)
     * @param currency      3-letter currency code (e.g. "INR")
     * @param metadata      key-value pairs stored on the PaymentIntent (bookingId, pnr)
     * @return the created PaymentIntent
     */
    public PaymentIntent createPaymentIntent(long amountInPaise, String currency,
                                              Map<String, String> metadata) {
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInPaise)
                    .setCurrency(currency.toLowerCase())
                    .putAllMetadata(metadata)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            log.info("Stripe PaymentIntent created: id={}, amount={} {}, metadata={}",
                    intent.getId(), amountInPaise, currency, metadata);

            return intent;

        } catch (StripeException e) {
            log.error("Failed to create Stripe PaymentIntent: {}", e.getMessage(), e);
            throw new PaymentException("Failed to create payment — " + e.getMessage());
        }
    }

    /**
     * Verifies a Stripe webhook signature and parses the event.
     *
     * @param payload   raw request body (must not be parsed/modified)
     * @param sigHeader Stripe-Signature header value
     * @return verified Stripe Event
     * @throws PaymentException if signature verification fails
     */
    public Event verifyWebhookSignature(String payload, String sigHeader) {
        try {
            String webhookSecret = appProperties.getPayment().getStripeWebhookSecret();
            return Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Stripe webhook signature verification failed: {}", e.getMessage());
            throw new PaymentException("Invalid webhook signature");
        } catch (Exception e) {
            log.error("Failed to parse Stripe webhook event: {}", e.getMessage(), e);
            throw new PaymentException("Failed to parse webhook event");
        }
    }
}
