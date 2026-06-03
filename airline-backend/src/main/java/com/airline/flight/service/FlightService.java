package com.airline.flight.service;

import com.airline.config.CacheConfig;
import com.airline.exception.ResourceNotFoundException;
import com.airline.flight.dto.FlightDTO;
import com.airline.flight.dto.FlightSearchRequest;
import com.airline.flight.entity.Flight;
import com.airline.flight.mapper.FlightMapper;
import com.airline.flight.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FlightService {

    private final FlightRepository flightRepository;
    private final FlightMapper flightMapper;

    /**
     * Core search: origin → destination on a date.
     * Cached in Redis for 5 minutes — key is "origin:dest:date".
     */
    @Cacheable(value = CacheConfig.FLIGHT_SEARCH_CACHE,
               key = "#request.origin.toUpperCase() + ':' + #request.destination.toUpperCase() + ':' + #request.date")
    public List<FlightDTO> searchFlights(FlightSearchRequest request) {
        String origin = request.getOrigin().toUpperCase().trim();
        String dest = request.getDestination().toUpperCase().trim();
        LocalDate date = request.getDate();

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        log.info("Searching flights (cache MISS): {} → {} on {}", origin, dest, date);

        List<Flight> flights = flightRepository.searchByIataCodes(origin, dest, startOfDay, endOfDay);

        log.info("Found {} flights for {} → {} on {}", flights.size(), origin, dest, date);

        return flights.stream()
                .map(flightMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get single flight details by ID.
     */
    public FlightDTO getFlightById(Long id) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flight", "id", id));
        return flightMapper.toDTO(flight);
    }

    /**
     * Get flight entity (for internal use by booking service).
     */
    public Flight getFlightEntity(Long id) {
        return flightRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flight", "id", id));
    }
}
