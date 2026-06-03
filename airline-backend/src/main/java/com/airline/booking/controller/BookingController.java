package com.airline.booking.controller;

import com.airline.booking.dto.BookingDTO;
import com.airline.booking.dto.CreateBookingRequest;
import com.airline.booking.service.BookingService;
import com.airline.common.ApiResponse;
import com.airline.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    /**
     * POST /api/bookings
     * Create a new booking with passenger details and seat selections.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BookingDTO>> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            Authentication authentication) {

        Long userId = ((User) authentication.getPrincipal()).getId();
        BookingDTO booking = bookingService.createBooking(request, userId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(booking, "Booking created — PNR: " + booking.getPnr()));
    }

    /**
     * GET /api/bookings/my-trips
     * Get all bookings for the current user (My Trips page).
     */
    @GetMapping("/my-trips")
    public ResponseEntity<ApiResponse<List<BookingDTO>>> getMyBookings(
            Authentication authentication) {

        Long userId = ((User) authentication.getPrincipal()).getId();
        List<BookingDTO> bookings = bookingService.getUserBookings(userId);
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    /**
     * GET /api/bookings/pnr/{pnr}
     * Look up a booking by PNR code.
     */
    @GetMapping("/pnr/{pnr}")
    public ResponseEntity<ApiResponse<BookingDTO>> getByPnr(@PathVariable String pnr) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.getBookingByPnr(pnr)));
    }

    /**
     * PUT /api/bookings/{id}/cancel
     * Cancel a booking.
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingDTO>> cancelBooking(
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = ((User) authentication.getPrincipal()).getId();
        BookingDTO cancelled = bookingService.cancelBooking(id, userId);
        return ResponseEntity.ok(ApiResponse.success(cancelled, "Booking cancelled"));
    }
}
