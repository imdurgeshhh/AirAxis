package com.airline.payment.event;

import com.airline.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Publishes domain events to RabbitMQ.
 * Uses @Async so publishing doesn't block the payment transaction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publishes a BookingConfirmedEvent after successful payment.
     * Async — runs on a separate thread so the HTTP response isn't delayed.
     */
    @Async
    public void publishBookingConfirmed(BookingConfirmedEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.AIRLINE_EXCHANGE,
                    RabbitMQConfig.BOOKING_CONFIRMED_KEY,
                    event
            );
            log.info("Published BookingConfirmedEvent: PNR={}, bookingId={}, email={}",
                    event.getPnr(), event.getBookingId(), event.getEmail());
        } catch (Exception e) {
            // Don't fail the payment flow if messaging is down.
            // The booking is already confirmed in the DB — emails can be retried.
            log.error("Failed to publish BookingConfirmedEvent for PNR={}: {}",
                    event.getPnr(), e.getMessage(), e);
        }
    }
}
