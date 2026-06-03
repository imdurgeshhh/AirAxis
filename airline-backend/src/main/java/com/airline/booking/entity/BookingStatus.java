package com.airline.booking.entity;

/**
 * Booking lifecycle statuses.
 */
public enum BookingStatus {
    PENDING,       // Created, awaiting payment
    CONFIRMED,     // Payment received, PNR issued
    CANCELLED,     // Cancelled by user or system
    REFUNDED       // Payment refunded
}
