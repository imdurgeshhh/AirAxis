package com.airline.booking.service;

import com.airline.booking.dto.*;
import com.airline.booking.entity.Booking;
import com.airline.booking.entity.BookingStatus;
import com.airline.booking.entity.Passenger;
import com.airline.booking.repository.BookingRepository;
import com.airline.config.AppProperties;
import com.airline.exception.BookingException;
import com.airline.exception.ResourceNotFoundException;
import com.airline.flight.entity.Flight;
import com.airline.flight.service.FlightService;
import com.airline.seat.entity.Seat;
import com.airline.seat.repository.SeatRepository;
import com.airline.seat.service.SeatService;
import com.airline.user.entity.User;
import com.airline.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SeatRepository seatRepository;
    private final SeatService seatService;
    private final FlightService flightService;
    private final UserRepository userRepository;
    private final AppProperties appProperties;

    private static final String PNR_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // No I,O,0,1 for readability
    private static final SecureRandom RANDOM = new SecureRandom();

    // =============================================
    // CREATE BOOKING (Step 5-8)
    // =============================================
    @Transactional
    public BookingDTO createBooking(CreateBookingRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Flight flight = flightService.getFlightEntity(request.getFlightId());

        // Validate all seats exist and belong to this flight
        List<Seat> seats = request.getPassengers().stream()
                .map(p -> {
                    Seat seat = seatRepository.findById(p.getSeatId())
                            .orElseThrow(() -> new ResourceNotFoundException("Seat", "id", p.getSeatId()));
                    if (!seat.getFlight().getId().equals(flight.getId())) {
                        throw new BookingException("Seat " + seat.getSeatNumber() + " does not belong to this flight");
                    }
                    return seat;
                })
                .collect(Collectors.toList());

        // Calculate total amount
        BigDecimal totalAmount = seats.stream()
                .map(seat -> switch (seat.getSeatClass()) {
                    case ECONOMY -> flight.getEconomyPrice();
                    case BUSINESS -> flight.getBusinessPrice();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Generate unique PNR
        String pnr = generateUniquePnr();

        // Create booking
        Booking booking = Booking.builder()
                .pnr(pnr)
                .user(user)
                .flight(flight)
                .bookingStatus(BookingStatus.PENDING)
                .totalAmount(totalAmount)
                .build();

        // Add passengers
        for (int i = 0; i < request.getPassengers().size(); i++) {
            PassengerRequest pr = request.getPassengers().get(i);
            Seat seat = seats.get(i);

            Passenger passenger = Passenger.builder()
                    .firstName(pr.getFirstName())
                    .lastName(pr.getLastName())
                    .dateOfBirth(pr.getDateOfBirth())
                    .passportNumber(pr.getPassportNumber())
                    .nationality(pr.getNationality())
                    .seat(seat)
                    .build();

            booking.addPassenger(passenger);

            // Mark seat as booked
            seatService.markSeatAsBooked(seat.getId());
        }

        booking = bookingRepository.save(booking);
        log.info("Booking created: PNR={}, user={}, flight={}, amount={}",
                pnr, userId, flight.getFlightNumber(), totalAmount);

        return toDTO(booking);
    }

    // =============================================
    // GET BOOKING BY PNR
    // =============================================
    @Transactional(readOnly = true)
    public BookingDTO getBookingByPnr(String pnr) {
        Booking booking = bookingRepository.findByPnr(pnr.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "PNR", pnr));
        return toDTO(booking);
    }

    // =============================================
    // GET USER BOOKINGS (My Trips)
    // =============================================
    @Transactional(readOnly = true)
    public List<BookingDTO> getUserBookings(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // =============================================
    // CANCEL BOOKING
    // =============================================
    @Transactional
    public BookingDTO cancelBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        if (!booking.getUser().getId().equals(userId)) {
            throw new BookingException("You can only cancel your own bookings");
        }

        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new BookingException("Booking is already cancelled");
        }

        booking.setBookingStatus(BookingStatus.CANCELLED);

        // Release seats
        booking.getPassengers().forEach(p -> {
            Seat seat = p.getSeat();
            seat.setAvailable(true);
            seat.setLockedUntil(null);
            seat.setLockedByUser(null);
            seatRepository.save(seat);
        });

        booking = bookingRepository.save(booking);
        log.info("Booking cancelled: PNR={}, user={}", booking.getPnr(), userId);

        return toDTO(booking);
    }

    // =============================================
    // CONFIRM BOOKING (called by PaymentService after payment)
    // =============================================
    @Transactional
    public void confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        booking.setBookingStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
        log.info("Booking confirmed: PNR={}", booking.getPnr());
    }

    // ---- Helpers ----

    private String generateUniquePnr() {
        int length = appProperties.getBooking().getPnrLength();
        String pnr;
        do {
            StringBuilder sb = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                sb.append(PNR_CHARS.charAt(RANDOM.nextInt(PNR_CHARS.length())));
            }
            pnr = sb.toString();
        } while (bookingRepository.findByPnr(pnr).isPresent());  // Ensure uniqueness
        return pnr;
    }

    private BookingDTO toDTO(Booking booking) {
        Flight flight = booking.getFlight();

        List<BookingDTO.PassengerDTO> passengerDTOs = booking.getPassengers().stream()
                .map(p -> BookingDTO.PassengerDTO.builder()
                        .id(p.getId())
                        .firstName(p.getFirstName())
                        .lastName(p.getLastName())
                        .dateOfBirth(p.getDateOfBirth())
                        .passportNumber(p.getPassportNumber())
                        .nationality(p.getNationality())
                        .seatNumber(p.getSeat().getSeatNumber())
                        .seatClass(p.getSeat().getSeatClass().name())
                        .build())
                .collect(Collectors.toList());

        return BookingDTO.builder()
                .id(booking.getId())
                .pnr(booking.getPnr())
                .bookingStatus(booking.getBookingStatus().name())
                .totalAmount(booking.getTotalAmount())
                .createdAt(booking.getCreatedAt())
                .flightNumber(flight.getFlightNumber())
                .originIataCode(flight.getOriginAirport().getIataCode())
                .originCity(flight.getOriginAirport().getCity())
                .destIataCode(flight.getDestinationAirport().getIataCode())
                .destCity(flight.getDestinationAirport().getCity())
                .departureTime(flight.getDepartureTime())
                .arrivalTime(flight.getArrivalTime())
                .passengers(passengerDTOs)
                .build();
    }
}
