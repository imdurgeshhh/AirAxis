package com.airline.flight.repository;

import com.airline.flight.entity.Flight;
import com.airline.flight.entity.FlightStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FlightRepository extends JpaRepository<Flight, Long> {

    Optional<Flight> findByFlightNumber(String flightNumber);

    /**
     * Core search query — origin → destination on a specific date.
     * Matches flights departing within the given date range (typically one day).
     * Only returns SCHEDULED flights.
     */
    @Query("""
        SELECT f FROM Flight f
        JOIN FETCH f.originAirport
        JOIN FETCH f.destinationAirport
        JOIN FETCH f.aircraft
        WHERE f.originAirport.id = :originId
          AND f.destinationAirport.id = :destId
          AND f.departureTime >= :startOfDay
          AND f.departureTime < :endOfDay
          AND f.status = :status
        ORDER BY f.departureTime ASC
    """)
    List<Flight> searchFlights(
            @Param("originId") Long originId,
            @Param("destId") Long destinationId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay,
            @Param("status") FlightStatus status
    );

    /**
     * Search by IATA codes instead of IDs (convenience for API).
     */
    @Query("""
        SELECT f FROM Flight f
        JOIN FETCH f.originAirport oa
        JOIN FETCH f.destinationAirport da
        JOIN FETCH f.aircraft
        WHERE oa.iataCode = :origin
          AND da.iataCode = :dest
          AND f.departureTime >= :startOfDay
          AND f.departureTime < :endOfDay
          AND f.status = 'SCHEDULED'
        ORDER BY f.departureTime ASC
    """)
    List<Flight> searchByIataCodes(
            @Param("origin") String originIata,
            @Param("dest") String destIata,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );

    List<Flight> findByStatus(FlightStatus status);
}
