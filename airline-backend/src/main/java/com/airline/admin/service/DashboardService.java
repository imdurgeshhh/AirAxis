package com.airline.admin.service;

import com.airline.admin.dto.DashboardDTO;
import com.airline.booking.dto.BookingDTO;
import com.airline.booking.entity.Booking;
import com.airline.booking.entity.BookingStatus;
import com.airline.booking.repository.BookingRepository;
import com.airline.flight.repository.FlightRepository;
import com.airline.payment.entity.PaymentStatus;
import com.airline.payment.repository.PaymentRepository;
import com.airline.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final UserRepository userRepository;
    private final FlightRepository flightRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    /**
     * Aggregates dashboard statistics across all entities.
     */
    public DashboardDTO getDashboard() {

        long totalUsers = userRepository.count();
        long totalFlights = flightRepository.count();
        long totalBookings = bookingRepository.count();

        // Total revenue = sum of all SUCCESS payments
        BigDecimal totalRevenue = paymentRepository.findAll().stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .map(p -> p.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Bookings by status
        Map<String, Long> bookingsByStatus = new LinkedHashMap<>();
        for (BookingStatus status : BookingStatus.values()) {
            long count = bookingRepository.findByBookingStatus(status).size();
            bookingsByStatus.put(status.name(), count);
        }

        // Recent bookings (last 10)
        List<BookingDTO> recentBookings = bookingRepository.findAll(
                        PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent()
                .stream()
                .map(this::toBookingDTO)
                .collect(Collectors.toList());

        return DashboardDTO.builder()
                .totalUsers(totalUsers)
                .totalFlights(totalFlights)
                .totalBookings(totalBookings)
                .totalRevenue(totalRevenue)
                .bookingsByStatus(bookingsByStatus)
                .recentBookings(recentBookings)
                .build();
    }

    /**
     * Get all bookings (paginated) for admin view.
     */
    public List<BookingDTO> getAllBookings(int page, int size) {
        return bookingRepository.findAll(
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent()
                .stream()
                .map(this::toBookingDTO)
                .collect(Collectors.toList());
    }

    // ---- Helpers ----

    private BookingDTO toBookingDTO(Booking booking) {
        return BookingDTO.builder()
                .id(booking.getId())
                .pnr(booking.getPnr())
                .bookingStatus(booking.getBookingStatus().name())
                .totalAmount(booking.getTotalAmount())
                .createdAt(booking.getCreatedAt())
                .flightNumber(booking.getFlight().getFlightNumber())
                .build();
    }
}
