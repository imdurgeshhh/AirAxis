package com.airline.admin.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request to create a new flight.
 * Seats are auto-generated from the aircraft's seat configuration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFlightRequest {

    @NotBlank(message = "Flight number is required (e.g. AI-101)")
    private String flightNumber;

    @NotNull(message = "Origin airport ID is required")
    private Long originAirportId;

    @NotNull(message = "Destination airport ID is required")
    private Long destinationAirportId;

    @NotNull(message = "Aircraft ID is required")
    private Long aircraftId;

    @NotNull(message = "Departure time is required")
    private LocalDateTime departureTime;

    @NotNull(message = "Arrival time is required")
    private LocalDateTime arrivalTime;

    @NotNull(message = "Economy price is required")
    @DecimalMin(value = "0.01", message = "Economy price must be positive")
    private BigDecimal economyPrice;

    @NotNull(message = "Business price is required")
    @DecimalMin(value = "0.01", message = "Business price must be positive")
    private BigDecimal businessPrice;
}
