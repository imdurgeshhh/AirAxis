package com.airline.checkin;

import com.airline.common.ApiResponse;
import com.airline.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/checkin")
@RequiredArgsConstructor
public class CheckinController {

    private final CheckinService checkinService;

    /**
     * POST /api/checkin
     * Web check-in for a booking. Opens 24h before departure.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<List<CheckinResponseDTO>>> checkIn(
            @Valid @RequestBody CheckinRequest request,
            Authentication authentication) {

        Long userId = ((User) authentication.getPrincipal()).getId();
        List<CheckinResponseDTO> results = checkinService.checkIn(request, userId);

        return ResponseEntity.ok(ApiResponse.success(results,
                results.size() + " passenger(s) checked in — boarding pass issued"));
    }

    /**
     * GET /api/checkin/status/{pnr}
     * Check check-in status for a booking.
     */
    @GetMapping("/status/{pnr}")
    public ResponseEntity<ApiResponse<List<CheckinResponseDTO>>> getStatus(
            @PathVariable String pnr) {

        List<CheckinResponseDTO> status = checkinService.getCheckinStatus(pnr);
        return ResponseEntity.ok(ApiResponse.success(status));
    }
}
