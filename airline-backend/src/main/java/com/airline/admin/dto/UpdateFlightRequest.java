package com.airline.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Partial update request for flights.
 * Only non-null fields are updated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFlightRequest {

    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private BigDecimal economyPrice;
    private BigDecimal businessPrice;
    private String status;       // SCHEDULED, DELAYED, CANCELLED, COMPLETED
}
