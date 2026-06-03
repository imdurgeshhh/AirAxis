package com.airline.seat.repository;

import com.airline.seat.entity.Seat;
import com.airline.seat.entity.SeatClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    /**
     * Get all seats for a flight (seat map).
     */
    List<Seat> findByFlightIdOrderBySeatNumber(Long flightId);

    /**
     * Get available seats by flight and class.
     */
    List<Seat> findByFlightIdAndSeatClassAndAvailableTrue(Long flightId, SeatClass seatClass);

    /**
     * Find a specific seat on a flight.
     */
    Optional<Seat> findByFlightIdAndSeatNumber(Long flightId, String seatNumber);

    /**
     * Release expired seat locks (cleanup job).
     */
    @Modifying
    @Query("""
        UPDATE Seat s SET s.available = true, s.lockedUntil = null, s.lockedByUser = null
        WHERE s.lockedUntil IS NOT NULL AND s.lockedUntil < :now AND s.available = false
    """)
    int releaseExpiredLocks(@Param("now") LocalDateTime now);
}
