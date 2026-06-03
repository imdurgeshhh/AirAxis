-- =============================================
-- V3__add_checkin_fields.sql
-- Adds check-in support to the passengers table
-- =============================================

ALTER TABLE passengers ADD COLUMN IF NOT EXISTS checked_in BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE passengers ADD COLUMN IF NOT EXISTS checked_in_at TIMESTAMP;
ALTER TABLE passengers ADD COLUMN IF NOT EXISTS boarding_pass_number VARCHAR(20);
