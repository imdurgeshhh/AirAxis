package com.airline.seat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatDTO {

    private Long id;
    private String seatNumber;
    private String seatClass;    // ECONOMY or BUSINESS
    private boolean available;
    private boolean lockedByCurrentUser;  // true if this user has it locked
}
