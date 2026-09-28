-- ============================================================
-- V2__seed_data.sql
-- Seed data: admin user, sample hospitals, initial blood stock
-- Password for all users: Admin@1234 (BCrypt hashed, strength 12)
-- ============================================================

-- ─── SUPER ADMIN ─────────────────────────────────────────────
INSERT INTO users (id, full_name, email, phone, password_hash, role, state, is_verified, is_active)
VALUES (
    gen_random_uuid(),
    'Platform Administrator',
    'admin@nigeriahealth.gov.ng',
    '08000000001',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewEWHcW6E2vN2QZe',
    'SUPER_ADMIN',
    'Abuja',
    TRUE,
    TRUE
);

-- ─── HOSPITAL ADMINS ─────────────────────────────────────────
INSERT INTO users (id, full_name, email, phone, password_hash, role, state, lga, is_verified, is_active)
VALUES
    ('a1b2c3d4-0001-0001-0001-000000000001', 'Dr. Aminu Kano', 'admin@luth.edu.ng', '08012000001',
     '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewEWHcW6E2vN2QZe', 'HOSPITAL_ADMIN', 'Lagos', 'Eti-Osa', TRUE, TRUE),
    ('a1b2c3d4-0002-0002-0002-000000000002', 'Dr. Ngozi Obi', 'admin@unilag.edu.ng', '08012000002',
     '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewEWHcW6E2vN2QZe', 'HOSPITAL_ADMIN', 'Abuja', 'Municipal', TRUE, TRUE),
    ('a1b2c3d4-0003-0003-0003-000000000003', 'Dr. Emeka Eze', 'admin@unth.edu.ng', '08012000003',
     '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewEWHcW6E2vN2QZe', 'HOSPITAL_ADMIN', 'Enugu', 'Enugu North', TRUE, TRUE);

-- ─── HOSPITALS ───────────────────────────────────────────────
INSERT INTO hospitals (id, name, address, state, lga, latitude, longitude, contact_email, contact_phone, admin_user_id, is_approved, approved_at)
VALUES
    ('b1b2c3d4-0001-0001-0001-000000000001',
     'Lagos University Teaching Hospital (LUTH)',
     'Idi-Araba, Surulere, Lagos',
     'Lagos', 'Surulere', 6.5095, 3.3573,
     'admin@luth.edu.ng', '08012000001',
     'a1b2c3d4-0001-0001-0001-000000000001', TRUE, NOW()),

    ('b1b2c3d4-0002-0002-0002-000000000002',
     'National Hospital Abuja',
     'Central Business District, Abuja',
     'Abuja', 'Municipal', 9.0574, 7.4898,
     'admin@unilag.edu.ng', '08012000002',
     'a1b2c3d4-0002-0002-0002-000000000002', TRUE, NOW()),

    ('b1b2c3d4-0003-0003-0003-000000000003',
     'University of Nigeria Teaching Hospital (UNTH)',
     'Ituku-Ozalla, Enugu',
     'Enugu', 'Enugu North', 6.3302, 7.4539,
     'admin@unth.edu.ng', '08012000003',
     'a1b2c3d4-0003-0003-0003-000000000003', TRUE, NOW());

-- ─── INITIAL BLOOD STOCK ─────────────────────────────────────
-- LUTH Lagos
INSERT INTO blood_stock (hospital_id, blood_type, units_available) VALUES
    ('b1b2c3d4-0001-0001-0001-000000000001', 'O_POSITIVE',  12),
    ('b1b2c3d4-0001-0001-0001-000000000001', 'O_NEGATIVE',   3),
    ('b1b2c3d4-0001-0001-0001-000000000001', 'A_POSITIVE',   8),
    ('b1b2c3d4-0001-0001-0001-000000000001', 'A_NEGATIVE',   1),
    ('b1b2c3d4-0001-0001-0001-000000000001', 'B_POSITIVE',   5),
    ('b1b2c3d4-0001-0001-0001-000000000001', 'B_NEGATIVE',   2),
    ('b1b2c3d4-0001-0001-0001-000000000001', 'AB_POSITIVE',  4),
    ('b1b2c3d4-0001-0001-0001-000000000001', 'AB_NEGATIVE',  0);

-- National Hospital Abuja
INSERT INTO blood_stock (hospital_id, blood_type, units_available) VALUES
    ('b1b2c3d4-0002-0002-0002-000000000002', 'O_POSITIVE',   9),
    ('b1b2c3d4-0002-0002-0002-000000000002', 'O_NEGATIVE',   4),
    ('b1b2c3d4-0002-0002-0002-000000000002', 'A_POSITIVE',   6),
    ('b1b2c3d4-0002-0002-0002-000000000002', 'A_NEGATIVE',   2),
    ('b1b2c3d4-0002-0002-0002-000000000002', 'B_POSITIVE',   7),
    ('b1b2c3d4-0002-0002-0002-000000000002', 'B_NEGATIVE',   1),
    ('b1b2c3d4-0002-0002-0002-000000000002', 'AB_POSITIVE',  3),
    ('b1b2c3d4-0002-0002-0002-000000000002', 'AB_NEGATIVE',  0);

-- UNTH Enugu
INSERT INTO blood_stock (hospital_id, blood_type, units_available) VALUES
    ('b1b2c3d4-0003-0003-0003-000000000003', 'O_POSITIVE',  10),
    ('b1b2c3d4-0003-0003-0003-000000000003', 'O_NEGATIVE',   2),
    ('b1b2c3d4-0003-0003-0003-000000000003', 'A_POSITIVE',   5),
    ('b1b2c3d4-0003-0003-0003-000000000003', 'A_NEGATIVE',   1),
    ('b1b2c3d4-0003-0003-0003-000000000003', 'B_POSITIVE',   4),
    ('b1b2c3d4-0003-0003-0003-000000000003', 'B_NEGATIVE',   0),
    ('b1b2c3d4-0003-0003-0003-000000000003', 'AB_POSITIVE',  2),
    ('b1b2c3d4-0003-0003-0003-000000000003', 'AB_NEGATIVE',  0);

-- ─── SAMPLE DONOR USER ───────────────────────────────────────
INSERT INTO users (id, full_name, email, phone, password_hash, role, state, lga, is_verified, is_active)
VALUES (
    'c1b2c3d4-0001-0001-0001-000000000001',
    'Chukwuemeka Okonkwo',
    'donor@example.com',
    '08055000001',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewEWHcW6E2vN2QZe',
    'DONOR', 'Lagos', 'Surulere', TRUE, TRUE
);

INSERT INTO donors (user_id, blood_type, date_of_birth, weight_kg, state, lga, total_donations, is_eligible)
VALUES (
    'c1b2c3d4-0001-0001-0001-000000000001',
    'O_POSITIVE', '1990-05-15', 75.0, 'Lagos', 'Surulere', 3, TRUE
);
