package com.airline.flight.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Search request from the frontend.
 * Example: origin=DEL, destination=BLR, date=2026-06-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightSearchRequest {

    @NotBlank(message = "Origin airport IATA code is required")
    private String origin;

    @NotBlank(message = "Destination airport IATA code is required")
    private String destination;

    @NotNull(message = "Travel date is required")
    private LocalDate date;
}
