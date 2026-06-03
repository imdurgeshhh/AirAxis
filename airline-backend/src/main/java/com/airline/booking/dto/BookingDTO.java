package com.airline.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDTO {

    private Long id;
    private String pnr;
    private String bookingStatus;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;

    // Flight info (denormalized)
    private String flightNumber;
    private String originIataCode;
    private String originCity;
    private String destIataCode;
    private String destCity;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;

    // Passengers
    private List<PassengerDTO> passengers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PassengerDTO {
        private Long id;
        private String firstName;
        private String lastName;
        private LocalDate dateOfBirth;
        private String passportNumber;
        private String nationality;
        private String seatNumber;
        private String seatClass;
    }
}
