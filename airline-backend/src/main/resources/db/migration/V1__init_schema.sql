-- =============================================
-- V1__init_schema.sql
-- Airline Reservation System — Initial Schema
-- =============================================

-- EXTENSIONS
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =============================================
-- USERS
-- =============================================
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    phone           VARCHAR(20),
    role            VARCHAR(20) NOT NULL DEFAULT 'PASSENGER',  -- PASSENGER | ADMIN
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =============================================
-- AIRPORTS
-- =============================================
CREATE TABLE airports (
    id              BIGSERIAL PRIMARY KEY,
    iata_code       CHAR(3) NOT NULL UNIQUE,        -- BLR, DEL, BOM
    name            VARCHAR(255) NOT NULL,
    city            VARCHAR(100) NOT NULL,
    country         VARCHAR(100) NOT NULL,
    timezone        VARCHAR(50) NOT NULL DEFAULT 'UTC'
);

-- =============================================
-- AIRCRAFT
-- =============================================
CREATE TABLE aircraft (
    id              BIGSERIAL PRIMARY KEY,
    model           VARCHAR(100) NOT NULL,           -- Boeing 737, Airbus A320
    total_seats     INT NOT NULL,
    economy_seats   INT NOT NULL,
    business_seats  INT NOT NULL
);

-- =============================================
-- FLIGHTS
-- =============================================
CREATE TABLE flights (
    id                      BIGSERIAL PRIMARY KEY,
    flight_number           VARCHAR(10) NOT NULL UNIQUE,    -- AI-101
    origin_airport_id       BIGINT NOT NULL REFERENCES airports(id),
    destination_airport_id  BIGINT NOT NULL REFERENCES airports(id),
    aircraft_id             BIGINT NOT NULL REFERENCES aircraft(id),
    departure_time          TIMESTAMP NOT NULL,
    arrival_time            TIMESTAMP NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
                            -- SCHEDULED | DELAYED | CANCELLED | COMPLETED
    economy_price           DECIMAL(10,2) NOT NULL,
    business_price          DECIMAL(10,2) NOT NULL,
    available_economy_seats INT NOT NULL,
    available_business_seats INT NOT NULL,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_different_airports CHECK (origin_airport_id <> destination_airport_id),
    CONSTRAINT chk_arrival_after_departure CHECK (arrival_time > departure_time)
);

-- =============================================
-- SEATS
-- =============================================
CREATE TABLE seats (
    id              BIGSERIAL PRIMARY KEY,
    flight_id       BIGINT NOT NULL REFERENCES flights(id) ON DELETE CASCADE,
    seat_number     VARCHAR(5) NOT NULL,            -- 12A, 23B
    class           VARCHAR(10) NOT NULL,           -- ECONOMY | BUSINESS
    is_available    BOOLEAN NOT NULL DEFAULT TRUE,
    locked_until    TIMESTAMP,                      -- Fallback if Redis misses
    locked_by_user  BIGINT REFERENCES users(id),
    UNIQUE (flight_id, seat_number)
);

-- =============================================
-- BOOKINGS
-- =============================================
CREATE TABLE bookings (
    id              BIGSERIAL PRIMARY KEY,
    pnr             CHAR(6) NOT NULL UNIQUE,        -- AB4X9Z
    user_id         BIGINT NOT NULL REFERENCES users(id),
    flight_id       BIGINT NOT NULL REFERENCES flights(id),
    booking_status  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                    -- PENDING | CONFIRMED | CANCELLED | REFUNDED
    total_amount    DECIMAL(10,2) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =============================================
-- PASSENGERS (per booking — supports group bookings)
-- =============================================
CREATE TABLE passengers (
    id              BIGSERIAL PRIMARY KEY,
    booking_id      BIGINT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    seat_id         BIGINT NOT NULL REFERENCES seats(id),
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    date_of_birth   DATE NOT NULL,
    passport_number VARCHAR(20),
    nationality     VARCHAR(100),
    UNIQUE (booking_id, seat_id)
);

-- =============================================
-- PAYMENTS
-- =============================================
CREATE TABLE payments (
    id                      BIGSERIAL PRIMARY KEY,
    booking_id              BIGINT NOT NULL REFERENCES bookings(id),
    stripe_payment_intent   VARCHAR(255) UNIQUE,
    amount                  DECIMAL(10,2) NOT NULL,
    currency                CHAR(3) NOT NULL DEFAULT 'INR',
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                            -- PENDING | SUCCESS | FAILED | REFUNDED
    paid_at                 TIMESTAMP,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =============================================
-- INDEXES (for common query patterns)
-- =============================================

-- Flight search (origin → destination on a date) — the most frequent query
CREATE INDEX idx_flights_search ON flights(origin_airport_id, destination_airport_id, departure_time);
CREATE INDEX idx_flights_status ON flights(status);

-- Seat availability check per flight
CREATE INDEX idx_seats_flight ON seats(flight_id, is_available, class);

-- Booking lookup by user
CREATE INDEX idx_bookings_user ON bookings(user_id);
CREATE INDEX idx_bookings_pnr  ON bookings(pnr);

-- Payment lookup by booking
CREATE INDEX idx_payments_booking ON payments(booking_id);

-- =============================================
-- SEED: Sample airports
-- =============================================
INSERT INTO airports (iata_code, name, city, country, timezone) VALUES
  ('BLR', 'Kempegowda International Airport', 'Bengaluru', 'India', 'Asia/Kolkata'),
  ('DEL', 'Indira Gandhi International Airport', 'New Delhi', 'India', 'Asia/Kolkata'),
  ('BOM', 'Chhatrapati Shivaji Maharaj Airport', 'Mumbai', 'India', 'Asia/Kolkata'),
  ('MAA', 'Chennai International Airport', 'Chennai', 'India', 'Asia/Kolkata'),
  ('HYD', 'Rajiv Gandhi International Airport', 'Hyderabad', 'India', 'Asia/Kolkata');
