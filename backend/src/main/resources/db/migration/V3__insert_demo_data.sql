-- V3__insert_demo_data.sql
-- Demo data so the app is usable the moment it starts.
--
-- Passwords are real BCrypt(strength 12) hashes:
--   barber / barber12345      -> ADMIN
--   customer / customer12345  -> CUSTOMER
-- Change them before any real deployment.

-- =========================
-- Users
-- =========================
INSERT INTO users (uuid, username, password, firstname, lastname, email, phone, role_id, created_at, updated_at, deleted)
SELECT
    UUID_TO_BIN('4f6a1b2c-0001-4a1b-9c3d-000000000001'),
    'barber',
    '$2a$12$1Cu1rqfeaINXjerXw1MyR.aJAkA8sUws2xoRhNyq8jO8LadplpRea',
    'Nikos',
    'Papadopoulos',
    'nikos@barbershop.gr',
    '+302101234567',
    r.id,
    UTC_TIMESTAMP(), UTC_TIMESTAMP(), 0
FROM roles r WHERE r.name = 'ADMIN';

INSERT INTO users (uuid, username, password, firstname, lastname, email, phone, role_id, created_at, updated_at, deleted)
SELECT
    UUID_TO_BIN('4f6a1b2c-0002-4a1b-9c3d-000000000002'),
    'customer',
    '$2a$12$FW2KtRNDBQIGOq0DAuYop./gwV4It4NijqiJukwP7vK9sfyMjzCrO',
    'Giorgos',
    'Dimitriou',
    'giorgos@example.gr',
    '+306941234567',
    r.id,
    UTC_TIMESTAMP(), UTC_TIMESTAMP(), 0
FROM roles r WHERE r.name = 'CUSTOMER';

-- =========================
-- The barber and their customer
-- =========================
INSERT INTO barbers (uuid, user_id, shop_name, bio, address, slot_step_minutes, created_at, updated_at, deleted)
SELECT
    UUID_TO_BIN('4f6a1b2c-0003-4a1b-9c3d-000000000003'),
    u.id,
    'Nikos Barber Studio',
    'Classic cuts, hot-towel shaves and beard care. Fifteen years behind the chair.',
    'Ermou 42, Athens',
    15,
    UTC_TIMESTAMP(), UTC_TIMESTAMP(), 0
FROM users u WHERE u.username = 'barber';

INSERT INTO customers (uuid, user_id, created_at, updated_at, deleted)
SELECT
    UUID_TO_BIN('4f6a1b2c-0004-4a1b-9c3d-000000000004'),
    u.id,
    UTC_TIMESTAMP(), UTC_TIMESTAMP(), 0
FROM users u WHERE u.username = 'customer';

-- =========================
-- Services on offer
-- =========================
INSERT INTO barber_services (uuid, barber_id, name, description, duration_minutes, price, active, created_at, updated_at, deleted)
SELECT UUID_TO_BIN('4f6a1b2c-0010-4a1b-9c3d-000000000010'), b.id,
       'Haircut', 'Wash, cut and style.', 30, 15.00, 1, UTC_TIMESTAMP(), UTC_TIMESTAMP(), 0
FROM barbers b WHERE b.shop_name = 'Nikos Barber Studio';

INSERT INTO barber_services (uuid, barber_id, name, description, duration_minutes, price, active, created_at, updated_at, deleted)
SELECT UUID_TO_BIN('4f6a1b2c-0011-4a1b-9c3d-000000000011'), b.id,
       'Beard Trim', 'Shape-up and line work with a straight razor finish.', 20, 10.00, 1, UTC_TIMESTAMP(), UTC_TIMESTAMP(), 0
FROM barbers b WHERE b.shop_name = 'Nikos Barber Studio';

INSERT INTO barber_services (uuid, barber_id, name, description, duration_minutes, price, active, created_at, updated_at, deleted)
SELECT UUID_TO_BIN('4f6a1b2c-0012-4a1b-9c3d-000000000012'), b.id,
       'Haircut & Beard', 'The full sit-down: cut, beard and hot towel.', 50, 22.00, 1, UTC_TIMESTAMP(), UTC_TIMESTAMP(), 0
FROM barbers b WHERE b.shop_name = 'Nikos Barber Studio';

INSERT INTO barber_services (uuid, barber_id, name, description, duration_minutes, price, active, created_at, updated_at, deleted)
SELECT UUID_TO_BIN('4f6a1b2c-0013-4a1b-9c3d-000000000013'), b.id,
       'Kids Haircut', 'Under 12s.', 20, 10.00, 1, UTC_TIMESTAMP(), UTC_TIMESTAMP(), 0
FROM barbers b WHERE b.shop_name = 'Nikos Barber Studio';

-- =========================
-- Weekly schedule: Tue-Fri 09:00-14:00 & 17:00-21:00, Sat 09:00-15:00.
-- Two rows for a split day is how the lunch break is expressed - there is no
-- "break" concept, a gap between two blocks simply is the break.
-- =========================
INSERT INTO working_hours (barber_id, day_of_week, start_time, end_time, created_at, updated_at, deleted)
SELECT b.id, d.day, d.st, d.et, UTC_TIMESTAMP(), UTC_TIMESTAMP(), 0
FROM barbers b
JOIN (
    SELECT 'TUESDAY'   AS day, '09:00:00' AS st, '14:00:00' AS et
    UNION ALL SELECT 'TUESDAY',   '17:00:00', '21:00:00'
    UNION ALL SELECT 'WEDNESDAY', '09:00:00', '14:00:00'
    UNION ALL SELECT 'WEDNESDAY', '17:00:00', '21:00:00'
    UNION ALL SELECT 'THURSDAY',  '09:00:00', '14:00:00'
    UNION ALL SELECT 'THURSDAY',  '17:00:00', '21:00:00'
    UNION ALL SELECT 'FRIDAY',    '09:00:00', '14:00:00'
    UNION ALL SELECT 'FRIDAY',    '17:00:00', '21:00:00'
    UNION ALL SELECT 'SATURDAY',  '09:00:00', '15:00:00'
) d
WHERE b.shop_name = 'Nikos Barber Studio';

-- =========================
-- An active promotion for the landing page
-- =========================
INSERT INTO promotions (uuid, barber_id, title, description, discount_percent, valid_from, valid_to, active, created_at, updated_at, deleted)
SELECT
    UUID_TO_BIN('4f6a1b2c-0020-4a1b-9c3d-000000000020'),
    b.id,
    'Midweek Special',
    'Twenty percent off the full cut-and-beard sit-down, Tuesday to Thursday.',
    20,
    '2026-01-01',
    '2026-12-31',
    1,
    UTC_TIMESTAMP(), UTC_TIMESTAMP(), 0
FROM barbers b WHERE b.shop_name = 'Nikos Barber Studio';

INSERT INTO promotions_services (promotion_id, barber_service_id)
SELECT p.id, s.id
FROM promotions p
JOIN barber_services s ON s.name IN ('Haircut & Beard', 'Beard Trim')
WHERE p.title = 'Midweek Special';
