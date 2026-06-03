package com.airline.aircraft.service;

import com.airline.aircraft.dto.AircraftDTO;
import com.airline.aircraft.dto.CreateAircraftRequest;
import com.airline.aircraft.entity.Aircraft;
import com.airline.aircraft.mapper.AircraftMapper;
import com.airline.aircraft.repository.AircraftRepository;
import com.airline.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AircraftService {

    private final AircraftRepository aircraftRepository;
    private final AircraftMapper aircraftMapper;

    public List<AircraftDTO> getAllAircraft() {
        return aircraftRepository.findAll()
                .stream()
                .map(aircraftMapper::toDTO)
                .collect(Collectors.toList());
    }

    public AircraftDTO getById(Long id) {
        return aircraftRepository.findById(id)
                .map(aircraftMapper::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Aircraft", "id", id));
    }

    public Aircraft getEntity(Long id) {
        return aircraftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aircraft", "id", id));
    }

    @Transactional
    public AircraftDTO create(CreateAircraftRequest request) {
        Aircraft aircraft = Aircraft.builder()
                .model(request.getModel().trim())
                .economySeats(request.getEconomySeats())
                .businessSeats(request.getBusinessSeats())
                .totalSeats(request.getEconomySeats() + request.getBusinessSeats())
                .build();

        aircraft = aircraftRepository.save(aircraft);
        log.info("Aircraft created: id={}, model={}, total={}",
                aircraft.getId(), aircraft.getModel(), aircraft.getTotalSeats());

        return aircraftMapper.toDTO(aircraft);
    }

    @Transactional
    public AircraftDTO update(Long id, CreateAircraftRequest request) {
        Aircraft aircraft = aircraftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aircraft", "id", id));

        aircraft.setModel(request.getModel().trim());
        aircraft.setEconomySeats(request.getEconomySeats());
        aircraft.setBusinessSeats(request.getBusinessSeats());
        aircraft.setTotalSeats(request.getEconomySeats() + request.getBusinessSeats());

        aircraft = aircraftRepository.save(aircraft);
        log.info("Aircraft updated: id={}, model={}", aircraft.getId(), aircraft.getModel());

        return aircraftMapper.toDTO(aircraft);
    }

    @Transactional
    public void delete(Long id) {
        if (!aircraftRepository.existsById(id)) {
            throw new ResourceNotFoundException("Aircraft", "id", id);
        }
        aircraftRepository.deleteById(id);
        log.info("Aircraft deleted: id={}", id);
    }
}
