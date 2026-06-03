package com.airline.flight.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Flight details returned in search results and flight detail pages.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightDTO {

    private Long id;
    private String flightNumber;

    // Origin airport
    private String originIataCode;
    private String originCity;
    private String originAirportName;

    // Destination airport
    private String destIataCode;
    private String destCity;
    private String destAirportName;

    // Aircraft
    private String aircraftModel;

    // Schedule
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private long durationMinutes;

    // Status
    private String status;

    // Pricing
    private BigDecimal economyPrice;
    private BigDecimal businessPrice;

    // Availability
    private int availableEconomySeats;
    private int availableBusinessSeats;
}
