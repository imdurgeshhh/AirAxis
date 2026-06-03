package com.airline.payment.repository;

import com.airline.payment.entity.Payment;
import com.airline.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Lookup by Stripe PaymentIntent ID — used for webhook idempotency.
     */
    Optional<Payment> findByStripePaymentIntent(String stripePaymentIntent);

    /**
     * Check if a payment already exists for a booking with a given status.
     * Prevents duplicate PaymentIntent creation.
     */
    Optional<Payment> findByBookingIdAndStatus(Long bookingId, PaymentStatus status);

    /**
     * Find the latest payment for a booking (regardless of status).
     */
    Optional<Payment> findTopByBookingIdOrderByCreatedAtDesc(Long bookingId);
}
