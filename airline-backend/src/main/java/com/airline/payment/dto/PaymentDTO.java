package com.airline.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment response DTO.
 * Includes clientSecret for Stripe.js frontend confirmation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDTO {

    private Long id;
    private Long bookingId;
    private String pnr;
    private String clientSecret;       // Stripe client_secret for frontend
    private BigDecimal amount;
    private String currency;
    private String status;
    private LocalDateTime createdAt;
}
