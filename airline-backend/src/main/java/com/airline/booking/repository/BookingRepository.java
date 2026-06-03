package com.airline.booking.repository;

import com.airline.booking.entity.Booking;
import com.airline.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.flight f
        JOIN FETCH f.originAirport
        JOIN FETCH f.destinationAirport
        LEFT JOIN FETCH b.passengers p
        LEFT JOIN FETCH p.seat
        WHERE b.pnr = :pnr
    """)
    Optional<Booking> findByPnr(@Param("pnr") String pnr);

    @Query("""
        SELECT DISTINCT b FROM Booking b
        JOIN FETCH b.flight f
        JOIN FETCH f.originAirport
        JOIN FETCH f.destinationAirport
        WHERE b.user.id = :userId
        ORDER BY b.createdAt DESC
    """)
    List<Booking> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    List<Booking> findByBookingStatus(BookingStatus status);
}
