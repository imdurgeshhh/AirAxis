package com.airline.checkin;

import com.airline.booking.entity.Booking;
import com.airline.booking.entity.BookingStatus;
import com.airline.booking.entity.Passenger;
import com.airline.booking.repository.BookingRepository;
import com.airline.exception.BookingException;
import com.airline.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Web check-in service.
 * Rules:
 * - Booking must be CONFIRMED
 * - Check-in opens 24 hours before departure
 * - Check-in closes 1 hour before departure
 * - Each passenger gets a unique boarding pass number
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckinService {

    private final BookingRepository bookingRepository;
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final int CHECKIN_OPENS_HOURS_BEFORE = 24;
    private static final int CHECKIN_CLOSES_HOURS_BEFORE = 1;

    /**
     * Check in passengers for a booking.
     */
    @Transactional
    public List<CheckinResponseDTO> checkIn(CheckinRequest request, Long userId) {

        Booking booking = bookingRepository.findByPnr(request.getPnr().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "PNR", request.getPnr()));

        // Ownership check
        if (!booking.getUser().getId().equals(userId)) {
            throw new BookingException("You can only check in your own bookings");
        }

        // Status check
        if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new BookingException("Only CONFIRMED bookings can be checked in. Current: "
                    + booking.getBookingStatus());
        }

        // Time window check
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime departure = booking.getFlight().getDepartureTime();
        Duration timeUntilDeparture = Duration.between(now, departure);

        if (timeUntilDeparture.toHours() > CHECKIN_OPENS_HOURS_BEFORE) {
            throw new BookingException("Check-in opens 24 hours before departure. Departure: "
                    + departure + ". Try again closer to departure.");
        }

        if (timeUntilDeparture.toHours() < CHECKIN_CLOSES_HOURS_BEFORE) {
            throw new BookingException("Check-in has closed (closes 1 hour before departure).");
        }

        // Get passengers to check in
        List<Passenger> passengers;
        if (request.getPassengerIds() != null && !request.getPassengerIds().isEmpty()) {
            passengers = booking.getPassengers().stream()
                    .filter(p -> request.getPassengerIds().contains(p.getId()))
                    .collect(Collectors.toList());

            if (passengers.isEmpty()) {
                throw new BookingException("No matching passengers found for the given IDs");
            }
        } else {
            passengers = booking.getPassengers();
        }

        // Check in each passenger
        List<CheckinResponseDTO> results = passengers.stream()
                .map(p -> {
                    if (p.isCheckedIn()) {
                        // Already checked in — return existing boarding pass
                        return toDTO(p);
                    }

                    p.setCheckedIn(true);
                    p.setCheckedInAt(LocalDateTime.now());
                    p.setBoardingPassNumber(generateBoardingPassNumber());
                    return toDTO(p);
                })
                .collect(Collectors.toList());

        bookingRepository.save(booking);

        log.info("Check-in completed: PNR={}, {} passengers checked in",
                booking.getPnr(), results.size());

        return results;
    }

    /**
     * Get check-in status for a booking.
     */
    @Transactional(readOnly = true)
    public List<CheckinResponseDTO> getCheckinStatus(String pnr) {
        Booking booking = bookingRepository.findByPnr(pnr.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "PNR", pnr));

        return booking.getPassengers().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ---- Helpers ----

    private String generateBoardingPassNumber() {
        // Format: BP-XXXXXX (6 random alphanumeric chars)
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder("BP-");
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private CheckinResponseDTO toDTO(Passenger p) {
        return CheckinResponseDTO.builder()
                .passengerId(p.getId())
                .firstName(p.getFirstName())
                .lastName(p.getLastName())
                .seatNumber(p.getSeat().getSeatNumber())
                .seatClass(p.getSeat().getSeatClass().name())
                .boardingPassNumber(p.getBoardingPassNumber())
                .checkedInAt(p.getCheckedInAt())
                .build();
    }
}
