package com.airline.notification;

import com.airline.config.RabbitMQConfig;
import com.airline.payment.event.BookingConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ consumer that listens for BookingConfirmedEvent
 * and sends confirmation emails.
 *
 * Retry behavior:
 * - If sendBookingConfirmation() throws, RabbitMQ will nack + requeue.
 * - Spring AMQP's default retry (3 attempts with backoff) applies.
 * - After exhausting retries, the message goes to DLQ (if configured) or is discarded.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingConfirmedConsumer {

    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.BOOKING_CONFIRMED_QUEUE)
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        log.info("Received BookingConfirmedEvent: PNR={}, email={}, bookingId={}",
                event.getPnr(), event.getEmail(), event.getBookingId());

        try {
            emailService.sendBookingConfirmation(event);
        } catch (Exception e) {
            log.error("Error processing BookingConfirmedEvent for PNR={}: {}",
                    event.getPnr(), e.getMessage(), e);
            // Rethrow so RabbitMQ can retry (up to configured max attempts)
            throw e;
        }
    }
}
