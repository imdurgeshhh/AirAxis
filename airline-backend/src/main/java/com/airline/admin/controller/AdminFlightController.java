package com.airline.admin.controller;

import com.airline.admin.dto.CreateFlightRequest;
import com.airline.admin.dto.UpdateFlightRequest;
import com.airline.admin.service.AdminFlightService;
import com.airline.common.ApiResponse;
import com.airline.flight.dto.FlightDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/flights")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminFlightController {

    private final AdminFlightService adminFlightService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FlightDTO>>> getAllFlights() {
        return ResponseEntity.ok(ApiResponse.success(adminFlightService.getAllFlights()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FlightDTO>> getFlightById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminFlightService.getFlightById(id)));
    }

    /**
     * POST /api/admin/flights
     * Creates a flight and auto-generates seats from the aircraft config.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<FlightDTO>> createFlight(
            @Valid @RequestBody CreateFlightRequest request) {
        FlightDTO created = adminFlightService.createFlight(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Flight created with seats auto-generated"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FlightDTO>> updateFlight(
            @PathVariable Long id,
            @RequestBody UpdateFlightRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                adminFlightService.updateFlight(id, request), "Flight updated"));
    }

    /**
     * DELETE /api/admin/flights/{id}
     * Cancels a flight, cancels pending bookings, releases seats.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<FlightDTO>> cancelFlight(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                adminFlightService.cancelFlight(id), "Flight cancelled"));
    }
}
