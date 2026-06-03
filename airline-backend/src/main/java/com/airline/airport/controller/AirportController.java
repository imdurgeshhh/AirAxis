package com.airline.airport.controller;

import com.airline.airport.dto.AirportDTO;
import com.airline.airport.service.AirportService;
import com.airline.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/airports")
@RequiredArgsConstructor
public class AirportController {

    private final AirportService airportService;

    /**
     * GET /api/airports
     * List all airports (used for dropdowns/autocomplete).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AirportDTO>>> getAllAirports() {
        return ResponseEntity.ok(ApiResponse.success(airportService.getAllAirports()));
    }

    /**
     * GET /api/airports/search?city=Del
     * Search airports by city name (partial match).
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<AirportDTO>>> searchByCity(
            @RequestParam String city) {
        return ResponseEntity.ok(ApiResponse.success(airportService.searchByCity(city)));
    }

    /**
     * GET /api/airports/{iataCode}
     * Get airport by IATA code (e.g., DEL, BLR).
     */
    @GetMapping("/{iataCode}")
    public ResponseEntity<ApiResponse<AirportDTO>> getByIataCode(
            @PathVariable String iataCode) {
        return ResponseEntity.ok(ApiResponse.success(airportService.getByIataCode(iataCode)));
    }
}
