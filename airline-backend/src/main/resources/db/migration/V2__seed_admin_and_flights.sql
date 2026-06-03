-- =============================================
-- V2__seed_admin_and_flights.sql
-- Seeds: admin user, aircraft fleet, sample flights + seats
-- =============================================

-- =============================================
-- 1. ADMIN USER
--    Email: admin@airaxis.com
--    Password: Admin@123 (BCrypt hash, cost=12)
-- =============================================
INSERT INTO users (email, password, first_name, last_name, phone, role, enabled, created_at, updated_at)
VALUES (
    'admin@airaxis.com',
    '$2a$12$LJ3m4ks2YEKFBnBHbSvolOG2hXsCzFP6VZNwXPqfGkWzJmFJnWKXS',
    'Admin',
    'AirAxis',
    '+91-9000000000',
    'ADMIN',
    TRUE,
    NOW(),
    NOW()
) ON CONFLICT (email) DO NOTHING;

-- =============================================
-- 2. AIRCRAFT FLEET
-- =============================================
INSERT INTO aircraft (model, total_seats, economy_seats, business_seats) VALUES
    ('Boeing 737-800',     189, 177, 12),
    ('Airbus A320neo',     180, 168, 12),
    ('Airbus A321neo',     220, 196, 24)
ON CONFLICT DO NOTHING;

-- =============================================
-- 3. SAMPLE FLIGHTS (next 7 days)
--    Uses airport IDs from V1 seed (BLR=1, DEL=2, BOM=3, MAA=4, HYD=5)
--    Uses aircraft IDs from above (737=1, A320=2, A321=3)
-- =============================================

-- DEL → BOM (Morning)
INSERT INTO flights (flight_number, origin_airport_id, destination_airport_id, aircraft_id,
    departure_time, arrival_time, status, economy_price, business_price,
    available_economy_seats, available_business_seats, created_at, updated_at)
VALUES
    ('AX-101', 2, 3, 1, NOW() + INTERVAL '1 day' + TIME '06:00', NOW() + INTERVAL '1 day' + TIME '08:10', 'SCHEDULED', 4500.00, 12500.00, 177, 12, NOW(), NOW()),
    ('AX-102', 3, 2, 1, NOW() + INTERVAL '1 day' + TIME '09:30', NOW() + INTERVAL '1 day' + TIME '11:40', 'SCHEDULED', 4800.00, 13000.00, 177, 12, NOW(), NOW()),
    ('AX-201', 2, 1, 2, NOW() + INTERVAL '2 days' + TIME '07:00', NOW() + INTERVAL '2 days' + TIME '09:45', 'SCHEDULED', 5200.00, 14500.00, 168, 12, NOW(), NOW()),
    ('AX-202', 1, 2, 2, NOW() + INTERVAL '2 days' + TIME '14:00', NOW() + INTERVAL '2 days' + TIME '16:45', 'SCHEDULED', 5500.00, 15000.00, 168, 12, NOW(), NOW()),
    ('AX-301', 1, 3, 3, NOW() + INTERVAL '3 days' + TIME '08:00', NOW() + INTERVAL '3 days' + TIME '09:30', 'SCHEDULED', 3800.00, 10500.00, 196, 24, NOW(), NOW()),
    ('AX-302', 3, 1, 3, NOW() + INTERVAL '3 days' + TIME '17:00', NOW() + INTERVAL '3 days' + TIME '18:30', 'SCHEDULED', 4200.00, 11000.00, 196, 24, NOW(), NOW()),
    ('AX-401', 2, 4, 1, NOW() + INTERVAL '4 days' + TIME '10:00', NOW() + INTERVAL '4 days' + TIME '12:30', 'SCHEDULED', 5000.00, 13500.00, 177, 12, NOW(), NOW()),
    ('AX-402', 4, 2, 1, NOW() + INTERVAL '4 days' + TIME '15:00', NOW() + INTERVAL '4 days' + TIME '17:30', 'SCHEDULED', 4900.00, 13000.00, 177, 12, NOW(), NOW()),
    ('AX-501', 1, 5, 2, NOW() + INTERVAL '5 days' + TIME '06:30', NOW() + INTERVAL '5 days' + TIME '07:45', 'SCHEDULED', 3200.00, 9000.00, 168, 12, NOW(), NOW()),
    ('AX-502', 5, 1, 2, NOW() + INTERVAL '5 days' + TIME '19:00', NOW() + INTERVAL '5 days' + TIME '20:15', 'SCHEDULED', 3500.00, 9500.00, 168, 12, NOW(), NOW())
ON CONFLICT (flight_number) DO NOTHING;

-- =============================================
-- 4. AUTO-GENERATE SEATS FOR EACH FLIGHT
--    Business: rows 1-3, cols A-D (4 per row = 12 seats)
--    Economy:  rows 10+, cols A-F (6 per row)
--    Adjusts for aircraft type
-- =============================================

-- Helper: generate seats for Boeing 737 flights (12 biz + 177 eco)
-- Flights AX-101, AX-102, AX-401, AX-402 use aircraft_id=1

DO $$
DECLARE
    flt RECORD;
    row_num INT;
    col CHAR(1);
    biz_cols CHAR[] := ARRAY['A','B','C','D'];
    eco_cols CHAR[] := ARRAY['A','B','C','D','E','F'];
    biz_seats INT;
    eco_seats INT;
    biz_generated INT;
    eco_generated INT;
BEGIN
    FOR flt IN SELECT f.id, a.business_seats, a.economy_seats
               FROM flights f JOIN aircraft a ON f.aircraft_id = a.id
               WHERE NOT EXISTS (SELECT 1 FROM seats s WHERE s.flight_id = f.id)
    LOOP
        biz_seats := flt.business_seats;
        eco_seats := flt.economy_seats;

        -- Generate business seats (rows 1+, 4 per row)
        biz_generated := 0;
        row_num := 1;
        WHILE biz_generated < biz_seats LOOP
            FOREACH col IN ARRAY biz_cols LOOP
                EXIT WHEN biz_generated >= biz_seats;
                INSERT INTO seats (flight_id, seat_number, class, is_available)
                VALUES (flt.id, row_num || col, 'BUSINESS', TRUE);
                biz_generated := biz_generated + 1;
            END LOOP;
            row_num := row_num + 1;
        END LOOP;

        -- Generate economy seats (rows 10+, 6 per row)
        eco_generated := 0;
        row_num := 10;
        WHILE eco_generated < eco_seats LOOP
            FOREACH col IN ARRAY eco_cols LOOP
                EXIT WHEN eco_generated >= eco_seats;
                INSERT INTO seats (flight_id, seat_number, class, is_available)
                VALUES (flt.id, row_num || col, 'ECONOMY', TRUE);
                eco_generated := eco_generated + 1;
            END LOOP;
            row_num := row_num + 1;
        END LOOP;

    END LOOP;
END $$;
