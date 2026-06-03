package com.airline.checkin;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Check-in request — can check in one or more passengers from the same booking.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckinRequest {

    @NotNull(message = "Booking PNR is required")
    private String pnr;

    /**
     * Passenger IDs to check in.
     * If null/empty, all passengers in the booking are checked in.
     */
    private List<Long> passengerIds;
}
