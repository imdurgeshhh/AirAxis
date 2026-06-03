package com.airline.payment.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event published to RabbitMQ when a booking is confirmed after successful payment.
 * Consumed by the notification service to send confirmation emails.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingConfirmedEvent implements Serializable {

    private Long bookingId;
    private Long userId;
    private String email;
    private String pnr;
    private BigDecimal totalAmount;
    private String currency;
    private String flightNumber;
    private String origin;
    private String destination;
    private LocalDateTime departureTime;
}
