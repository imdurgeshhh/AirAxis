package com.airline.payment.entity;

/**
 * Payment lifecycle statuses — mirrors the DB CHECK constraint.
 */
public enum PaymentStatus {
    PENDING,    // PaymentIntent created, awaiting Stripe confirmation
    SUCCESS,    // Webhook confirmed payment succeeded
    FAILED,     // Webhook confirmed payment failed
    REFUNDED    // Payment refunded (future use)
}
