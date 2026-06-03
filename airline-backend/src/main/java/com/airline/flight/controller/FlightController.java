package com.airline.flight.controller;

import com.airline.common.ApiResponse;
import com.airline.flight.dto.FlightDTO;
import com.airline.flight.dto.FlightSearchRequest;
import com.airline.flight.service.FlightService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/flights")
@RequiredArgsConstructor
public class FlightController {

    private final FlightService flightService;

    /**
     * GET /api/flights/search?origin=DEL&destination=BLR&date=2026-06-15
     * Public endpoint — search for available flights.
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<FlightDTO>>> searchFlights(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        FlightSearchRequest request = FlightSearchRequest.builder()
                .origin(origin)
                .destination(destination)
                .date(date)
                .build();

        List<FlightDTO> results = flightService.searchFlights(request);
        return ResponseEntity.ok(ApiResponse.success(results,
                results.isEmpty() ? "No flights found" : results.size() + " flights found"));
    }

    /**
     * GET /api/flights/{id}
     * Public endpoint — get flight details.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FlightDTO>> getFlightById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(flightService.getFlightById(id)));
    }
}
