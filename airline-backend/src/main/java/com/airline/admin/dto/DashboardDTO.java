package com.airline.admin.dto;

import com.airline.booking.dto.BookingDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Admin dashboard aggregated statistics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDTO {

    private long totalUsers;
    private long totalFlights;
    private long totalBookings;
    private BigDecimal totalRevenue;

    // Breakdown: { "PENDING": 5, "CONFIRMED": 23, "CANCELLED": 3 }
    private Map<String, Long> bookingsByStatus;

    // Last 10 bookings
    private List<BookingDTO> recentBookings;
}
