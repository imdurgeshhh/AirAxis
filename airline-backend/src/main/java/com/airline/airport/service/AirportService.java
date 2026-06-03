package com.airline.airport.service;

import com.airline.airport.dto.AirportDTO;
import com.airline.airport.mapper.AirportMapper;
import com.airline.airport.repository.AirportRepository;
import com.airline.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AirportService {

    private final AirportRepository airportRepository;
    private final AirportMapper airportMapper;

    public List<AirportDTO> getAllAirports() {
        return airportRepository.findAll()
                .stream()
                .map(airportMapper::toDTO)
                .collect(Collectors.toList());
    }

    public AirportDTO getByIataCode(String iataCode) {
        return airportRepository.findByIataCode(iataCode.toUpperCase())
                .map(airportMapper::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Airport", "iataCode", iataCode));
    }

    public List<AirportDTO> searchByCity(String city) {
        return airportRepository.findByCityContainingIgnoreCase(city)
                .stream()
                .map(airportMapper::toDTO)
                .collect(Collectors.toList());
    }
}
