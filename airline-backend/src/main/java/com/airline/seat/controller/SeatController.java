package com.airline.seat.controller;

import com.airline.common.ApiResponse;
import com.airline.seat.dto.SeatDTO;
import com.airline.seat.service.SeatService;
import com.airline.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/flights/{flightId}/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    /**
     * GET /api/flights/{flightId}/seats
     * Public — get the seat map for a flight.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<SeatDTO>>> getSeatMap(
            @PathVariable Long flightId,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        List<SeatDTO> seats = seatService.getSeatMap(flightId, userId);
        return ResponseEntity.ok(ApiResponse.success(seats));
    }

    /**
     * POST /api/flights/{flightId}/seats/{seatId}/lock
     * Lock a seat for 10 minutes during checkout.
     */
    @PostMapping("/{seatId}/lock")
    public ResponseEntity<ApiResponse<SeatDTO>> lockSeat(
            @PathVariable Long flightId,
            @PathVariable Long seatId,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        SeatDTO locked = seatService.lockSeat(flightId, seatId, userId);
        return ResponseEntity.ok(ApiResponse.success(locked, "Seat locked for 10 minutes"));
    }

    /**
     * DELETE /api/flights/{flightId}/seats/{seatId}/lock
     * Release a seat lock.
     */
    @DeleteMapping("/{seatId}/lock")
    public ResponseEntity<ApiResponse<Void>> releaseSeat(
            @PathVariable Long flightId,
            @PathVariable Long seatId,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        seatService.releaseSeat(flightId, seatId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Seat lock released"));
    }

    private Long extractUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof User)) {
            return null;
        }
        return ((User) auth.getPrincipal()).getId();
    }
}
