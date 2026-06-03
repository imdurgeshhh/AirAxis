package com.airline.payment.service;

import com.airline.booking.entity.Booking;
import com.airline.booking.entity.BookingStatus;
import com.airline.booking.repository.BookingRepository;
import com.airline.booking.service.BookingService;
import com.airline.exception.PaymentException;
import com.airline.exception.ResourceNotFoundException;
import com.airline.payment.dto.CreatePaymentRequest;
import com.airline.payment.dto.PaymentDTO;
import com.airline.payment.entity.Payment;
import com.airline.payment.entity.PaymentStatus;
import com.airline.payment.event.BookingConfirmedEvent;
import com.airline.payment.event.EventPublisher;
import com.airline.payment.repository.PaymentRepository;
import com.airline.seat.entity.Seat;
import com.airline.seat.repository.SeatRepository;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Core payment orchestrator.
 *
 * Flow:
 * 1. createPayment()       — creates Stripe PaymentIntent + saves PENDING record
 * 2. handlePaymentSuccess() — webhook confirms → update payment, confirm booking, publish event
 * 3. handlePaymentFailure() — webhook failure → update payment, release seats
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final SeatRepository seatRepository;
    private final BookingService bookingService;
    private final StripeService stripeService;
    private final EventPublisher eventPublisher;

    // =============================================
    // 1. CREATE PAYMENT INTENT
    // =============================================

    /**
     * Creates a Stripe PaymentIntent and saves a PENDING payment record.
     * Returns clientSecret for frontend to complete payment via Stripe.js.
     *
     * @param request contains bookingId
     * @param userId  authenticated user ID (to verify ownership)
     * @return PaymentDTO with clientSecret
     */
    @Transactional
    public PaymentDTO createPayment(CreatePaymentRequest request, Long userId) {

        // 1. Load and validate booking
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", request.getBookingId()));

        if (!booking.getUser().getId().equals(userId)) {
            throw new PaymentException("You can only pay for your own bookings");
        }

        if (booking.getBookingStatus() != BookingStatus.PENDING) {
            throw new PaymentException("Booking is not in PENDING status — current: " + booking.getBookingStatus());
        }

        // 2. Idempotency — check if a SUCCESS payment already exists
        Optional<Payment> existingSuccess = paymentRepository
                .findByBookingIdAndStatus(request.getBookingId(), PaymentStatus.SUCCESS);
        if (existingSuccess.isPresent()) {
            throw new PaymentException("Payment already completed for this booking (PNR: " + booking.getPnr() + ")");
        }

        // 3. Check if a PENDING PaymentIntent already exists (re-attempt scenario)
        Optional<Payment> existingPending = paymentRepository
                .findByBookingIdAndStatus(request.getBookingId(), PaymentStatus.PENDING);
        if (existingPending.isPresent()) {
            Payment pending = existingPending.get();
            log.info("Returning existing PENDING payment for booking {}: intent={}",
                    booking.getPnr(), pending.getStripePaymentIntent());

            // Re-fetch the PaymentIntent to get a fresh clientSecret
            try {
                PaymentIntent existingIntent = PaymentIntent.retrieve(pending.getStripePaymentIntent());
                return toDTO(pending, existingIntent.getClientSecret());
            } catch (Exception e) {
                log.warn("Failed to retrieve existing PaymentIntent {}, creating new one",
                        pending.getStripePaymentIntent());
                // Fall through to create a new one — mark old as FAILED
                pending.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(pending);
            }
        }

        // 4. Convert amount to paise (smallest currency unit for INR)
        BigDecimal amount = booking.getTotalAmount();
        long amountInPaise = amount.multiply(BigDecimal.valueOf(100)).longValueExact();

        // 5. Create Stripe PaymentIntent with metadata
        Map<String, String> metadata = Map.of(
                "bookingId", booking.getId().toString(),
                "pnr", booking.getPnr(),
                "userId", userId.toString()
        );

        PaymentIntent intent = stripeService.createPaymentIntent(amountInPaise, "INR", metadata);

        // 6. Save payment record (PENDING)
        Payment payment = Payment.builder()
                .booking(booking)
                .stripePaymentIntent(intent.getId())
                .amount(amount)
                .currency("INR")
                .status(PaymentStatus.PENDING)
                .build();

        payment = paymentRepository.save(payment);

        log.info("Payment created: id={}, booking={}, intent={}, amount={}",
                payment.getId(), booking.getPnr(), intent.getId(), amount);

        return toDTO(payment, intent.getClientSecret());
    }

    // =============================================
    // 2. HANDLE PAYMENT SUCCESS (webhook)
    // =============================================

    /**
     * Called when Stripe webhook fires payment_intent.succeeded.
     * This is the SOURCE OF TRUTH — never trust frontend callbacks.
     *
     * Transactional: payment update + booking confirm + seat finalization
     * all succeed or all roll back together.
     */
    @Transactional
    public void handlePaymentSuccess(String stripePaymentIntentId) {

        // 1. Find payment by Stripe intent ID
        Payment payment = paymentRepository.findByStripePaymentIntent(stripePaymentIntentId)
                .orElseThrow(() -> {
                    log.warn("Received webhook for unknown PaymentIntent: {}", stripePaymentIntentId);
                    return new PaymentException("Unknown PaymentIntent: " + stripePaymentIntentId);
                });

        // 2. Idempotency — if already processed, skip silently (safe for webhook retries)
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Ignoring duplicate webhook for already-successful payment: intent={}, booking={}",
                    stripePaymentIntentId, payment.getBooking().getPnr());
            return;
        }

        // 3. Update payment → SUCCESS
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // 4. Confirm booking (changes status PENDING → CONFIRMED)
        Booking booking = payment.getBooking();
        bookingService.confirmBooking(booking.getId());

        log.info("Payment SUCCESS: intent={}, PNR={}, amount={} {}",
                stripePaymentIntentId, booking.getPnr(), payment.getAmount(), payment.getCurrency());

        // 5. Publish event to RabbitMQ (async — won't block this transaction)
        BookingConfirmedEvent event = BookingConfirmedEvent.builder()
                .bookingId(booking.getId())
                .userId(booking.getUser().getId())
                .email(booking.getUser().getEmail())
                .pnr(booking.getPnr())
                .totalAmount(payment.getAmount())
                .currency(payment.getCurrency())
                .flightNumber(booking.getFlight().getFlightNumber())
                .origin(booking.getFlight().getOriginAirport().getIataCode())
                .destination(booking.getFlight().getDestinationAirport().getIataCode())
                .departureTime(booking.getFlight().getDepartureTime())
                .build();

        eventPublisher.publishBookingConfirmed(event);
    }

    // =============================================
    // 3. HANDLE PAYMENT FAILURE (webhook)
    // =============================================

    /**
     * Called when Stripe webhook fires payment_intent.payment_failed.
     * Marks payment as FAILED and releases locked seats.
     */
    @Transactional
    public void handlePaymentFailure(String stripePaymentIntentId) {

        Payment payment = paymentRepository.findByStripePaymentIntent(stripePaymentIntentId)
                .orElseThrow(() -> {
                    log.warn("Received failure webhook for unknown PaymentIntent: {}", stripePaymentIntentId);
                    return new PaymentException("Unknown PaymentIntent: " + stripePaymentIntentId);
                });

        // Idempotency — skip if already terminal
        if (payment.getStatus() == PaymentStatus.FAILED || payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Ignoring duplicate failure webhook: intent={}, currentStatus={}",
                    stripePaymentIntentId, payment.getStatus());
            return;
        }

        // Mark payment as FAILED
        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);

        // Release locked seats for this booking
        Booking booking = payment.getBooking();
        booking.getPassengers().forEach(passenger -> {
            Seat seat = passenger.getSeat();
            seat.setAvailable(true);
            seat.setLockedUntil(null);
            seat.setLockedByUser(null);
            seatRepository.save(seat);
        });

        log.warn("Payment FAILED: intent={}, PNR={}", stripePaymentIntentId, booking.getPnr());
    }

    // ---- DTO Mapper ----

    private PaymentDTO toDTO(Payment payment, String clientSecret) {
        return PaymentDTO.builder()
                .id(payment.getId())
                .bookingId(payment.getBooking().getId())
                .pnr(payment.getBooking().getPnr())
                .clientSecret(clientSecret)
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus().name())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
