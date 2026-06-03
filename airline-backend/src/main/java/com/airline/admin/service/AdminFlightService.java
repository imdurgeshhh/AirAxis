package com.airline.admin.service;

import com.airline.admin.dto.CreateFlightRequest;
import com.airline.admin.dto.UpdateFlightRequest;
import com.airline.aircraft.entity.Aircraft;
import com.airline.aircraft.service.AircraftService;
import com.airline.airport.entity.Airport;
import com.airline.airport.repository.AirportRepository;
import com.airline.booking.entity.Booking;
import com.airline.booking.entity.BookingStatus;
import com.airline.booking.repository.BookingRepository;
import com.airline.config.CacheConfig;
import com.airline.exception.ResourceNotFoundException;
import com.airline.flight.dto.FlightDTO;
import com.airline.flight.entity.Flight;
import com.airline.flight.entity.FlightStatus;
import com.airline.flight.mapper.FlightMapper;
import com.airline.flight.repository.FlightRepository;
import com.airline.seat.entity.Seat;
import com.airline.seat.entity.SeatClass;
import com.airline.seat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminFlightService {

    private final FlightRepository flightRepository;
    private final AirportRepository airportRepository;
    private final AircraftService aircraftService;
    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;
    private final FlightMapper flightMapper;

    // Seat layout constants
    private static final char[] BUSINESS_COLS = {'A', 'B', 'C', 'D'};           // 4 per row
    private static final char[] ECONOMY_COLS  = {'A', 'B', 'C', 'D', 'E', 'F'}; // 6 per row
    private static final int BUSINESS_START_ROW = 1;
    private static final int ECONOMY_START_ROW  = 10;

    // =============================================
    // GET ALL FLIGHTS (admin — no status filter)
    // =============================================
    @Transactional(readOnly = true)
    public List<FlightDTO> getAllFlights() {
        return flightRepository.findAll()
                .stream()
                .map(flightMapper::toDTO)
                .collect(Collectors.toList());
    }

    // =============================================
    // GET FLIGHT BY ID (admin detail view)
    // =============================================
    @Transactional(readOnly = true)
    public FlightDTO getFlightById(Long id) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flight", "id", id));
        return flightMapper.toDTO(flight);
    }

    // =============================================
    // CREATE FLIGHT + AUTO-GENERATE SEATS
    // =============================================
    @Transactional
    @CacheEvict(value = CacheConfig.FLIGHT_SEARCH_CACHE, allEntries = true)
    public FlightDTO createFlight(CreateFlightRequest request) {

        // Validate airports
        Airport origin = airportRepository.findById(request.getOriginAirportId())
                .orElseThrow(() -> new ResourceNotFoundException("Airport", "id", request.getOriginAirportId()));
        Airport destination = airportRepository.findById(request.getDestinationAirportId())
                .orElseThrow(() -> new ResourceNotFoundException("Airport", "id", request.getDestinationAirportId()));

        if (origin.getId().equals(destination.getId())) {
            throw new IllegalArgumentException("Origin and destination airports must be different");
        }

        // Validate aircraft
        Aircraft aircraft = aircraftService.getEntity(request.getAircraftId());

        // Validate times
        if (!request.getArrivalTime().isAfter(request.getDepartureTime())) {
            throw new IllegalArgumentException("Arrival time must be after departure time");
        }

        // Check flight number uniqueness
        if (flightRepository.findByFlightNumber(request.getFlightNumber().toUpperCase().trim()).isPresent()) {
            throw new IllegalArgumentException("Flight number already exists: " + request.getFlightNumber());
        }

        // Create flight
        Flight flight = Flight.builder()
                .flightNumber(request.getFlightNumber().toUpperCase().trim())
                .originAirport(origin)
                .destinationAirport(destination)
                .aircraft(aircraft)
                .departureTime(request.getDepartureTime())
                .arrivalTime(request.getArrivalTime())
                .economyPrice(request.getEconomyPrice())
                .businessPrice(request.getBusinessPrice())
                .availableEconomySeats(aircraft.getEconomySeats())
                .availableBusinessSeats(aircraft.getBusinessSeats())
                .status(FlightStatus.SCHEDULED)
                .build();

        flight = flightRepository.save(flight);

        // Auto-generate seats
        List<Seat> seats = generateSeats(flight, aircraft);
        seatRepository.saveAll(seats);

        log.info("Flight created: {} ({} → {}), {} seats generated",
                flight.getFlightNumber(), origin.getIataCode(), destination.getIataCode(), seats.size());

        return flightMapper.toDTO(flight);
    }

    // =============================================
    // UPDATE FLIGHT (partial update)
    // =============================================
    @Transactional
    @CacheEvict(value = CacheConfig.FLIGHT_SEARCH_CACHE, allEntries = true)
    public FlightDTO updateFlight(Long id, UpdateFlightRequest request) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flight", "id", id));

        if (request.getDepartureTime() != null) {
            flight.setDepartureTime(request.getDepartureTime());
        }
        if (request.getArrivalTime() != null) {
            flight.setArrivalTime(request.getArrivalTime());
        }
        if (request.getEconomyPrice() != null) {
            flight.setEconomyPrice(request.getEconomyPrice());
        }
        if (request.getBusinessPrice() != null) {
            flight.setBusinessPrice(request.getBusinessPrice());
        }
        if (request.getStatus() != null) {
            try {
                flight.setStatus(FlightStatus.valueOf(request.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid flight status: " + request.getStatus());
            }
        }

        // Re-validate times if both are set
        if (flight.getArrivalTime() != null && flight.getDepartureTime() != null
                && !flight.getArrivalTime().isAfter(flight.getDepartureTime())) {
            throw new IllegalArgumentException("Arrival time must be after departure time");
        }

        flight = flightRepository.save(flight);
        log.info("Flight updated: {} (id={})", flight.getFlightNumber(), flight.getId());

        return flightMapper.toDTO(flight);
    }

    // =============================================
    // CANCEL FLIGHT
    // =============================================
    @Transactional
    @CacheEvict(value = CacheConfig.FLIGHT_SEARCH_CACHE, allEntries = true)
    public FlightDTO cancelFlight(Long id) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flight", "id", id));

        if (flight.getStatus() == FlightStatus.CANCELLED) {
            throw new IllegalArgumentException("Flight is already cancelled");
        }

        // Cancel flight
        flight.setStatus(FlightStatus.CANCELLED);
        flightRepository.save(flight);

        // Cancel all PENDING bookings for this flight
        List<Booking> pendingBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getFlight().getId().equals(id))
                .filter(b -> b.getBookingStatus() == BookingStatus.PENDING)
                .collect(Collectors.toList());

        for (Booking booking : pendingBookings) {
            booking.setBookingStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
        }

        // Release all seats on this flight
        List<Seat> seats = seatRepository.findByFlightIdOrderBySeatNumber(id);
        for (Seat seat : seats) {
            seat.setAvailable(true);
            seat.setLockedUntil(null);
            seat.setLockedByUser(null);
        }
        seatRepository.saveAll(seats);

        log.info("Flight cancelled: {}, {} pending bookings cancelled, {} seats released",
                flight.getFlightNumber(), pendingBookings.size(), seats.size());

        return flightMapper.toDTO(flight);
    }

    // =============================================
    // SEAT GENERATION LOGIC
    // =============================================

    /**
     * Generates seat rows based on aircraft configuration.
     *
     * Business class: 4 seats per row (A-D), starting at row 1
     *   e.g., 12 business seats → rows 1-3 → 1A,1B,1C,1D, 2A,...,3D
     *
     * Economy class: 6 seats per row (A-F), starting at row 10
     *   e.g., 150 economy seats → rows 10-34 → 10A,...,34F
     */
    private List<Seat> generateSeats(Flight flight, Aircraft aircraft) {
        List<Seat> seats = new ArrayList<>();

        // Business seats
        int bizCount = aircraft.getBusinessSeats();
        int bizRow = BUSINESS_START_ROW;
        int bizGenerated = 0;
        while (bizGenerated < bizCount) {
            for (char col : BUSINESS_COLS) {
                if (bizGenerated >= bizCount) break;
                seats.add(Seat.builder()
                        .flight(flight)
                        .seatNumber(bizRow + String.valueOf(col))
                        .seatClass(SeatClass.BUSINESS)
                        .available(true)
                        .build());
                bizGenerated++;
            }
            bizRow++;
        }

        // Economy seats
        int ecoCount = aircraft.getEconomySeats();
        int ecoRow = ECONOMY_START_ROW;
        int ecoGenerated = 0;
        while (ecoGenerated < ecoCount) {
            for (char col : ECONOMY_COLS) {
                if (ecoGenerated >= ecoCount) break;
                seats.add(Seat.builder()
                        .flight(flight)
                        .seatNumber(ecoRow + String.valueOf(col))
                        .seatClass(SeatClass.ECONOMY)
                        .available(true)
                        .build());
                ecoGenerated++;
            }
            ecoRow++;
        }

        return seats;
    }
}
