package com.airline.exception;

public class SeatAlreadyLockedException extends RuntimeException {
    public SeatAlreadyLockedException(String seatNumber) {
        super("Seat " + seatNumber + " is already selected by another user. Please choose a different seat.");
    }
}
