package com.airline.admin.controller;

import com.airline.admin.dto.CreateAirportRequest;
import com.airline.airport.dto.AirportDTO;
import com.airline.airport.entity.Airport;
import com.airline.airport.mapper.AirportMapper;
import com.airline.airport.repository.AirportRepository;
import com.airline.common.ApiResponse;
import com.airline.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/admin/airports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAirportController {

    private final AirportRepository airportRepository;
    private final AirportMapper airportMapper;

    @PostMapping
    public ResponseEntity<ApiResponse<AirportDTO>> create(
            @Valid @RequestBody CreateAirportRequest request) {

        if (airportRepository.findByIataCode(request.getIataCode().toUpperCase()).isPresent()) {
            throw new IllegalArgumentException("Airport with IATA code already exists: " + request.getIataCode());
        }

        Airport airport = Airport.builder()
                .iataCode(request.getIataCode().toUpperCase().trim())
                .name(request.getName().trim())
                .city(request.getCity().trim())
                .country(request.getCountry().trim())
                .timezone(request.getTimezone() != null ? request.getTimezone() : "UTC")
                .build();

        airport = airportRepository.save(airport);
        log.info("Airport created: {} ({})", airport.getIataCode(), airport.getCity());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(airportMapper.toDTO(airport), "Airport created"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AirportDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateAirportRequest request) {

        Airport airport = airportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Airport", "id", id));

        airport.setIataCode(request.getIataCode().toUpperCase().trim());
        airport.setName(request.getName().trim());
        airport.setCity(request.getCity().trim());
        airport.setCountry(request.getCountry().trim());
        if (request.getTimezone() != null) {
            airport.setTimezone(request.getTimezone());
        }

        airport = airportRepository.save(airport);
        log.info("Airport updated: {} (id={})", airport.getIataCode(), airport.getId());

        return ResponseEntity.ok(ApiResponse.success(airportMapper.toDTO(airport), "Airport updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        if (!airportRepository.existsById(id)) {
            throw new ResourceNotFoundException("Airport", "id", id);
        }
        airportRepository.deleteById(id);
        log.info("Airport deleted: id={}", id);
        return ResponseEntity.ok(ApiResponse.success(null, "Airport deleted"));
    }
}
