-- ============================================================
-- V1__init_schema.sql
-- Nigeria Health & Safety Platform — Blood Bank Schema
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ─── USERS ───────────────────────────────────────────────────
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL UNIQUE,
    phone         VARCHAR(20)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(30)  NOT NULL,
    state         VARCHAR(50),
    lga           VARCHAR(50),
    is_verified   BOOLEAN NOT NULL DEFAULT FALSE,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role  ON users(role);

-- ─── OTP TOKENS ──────────────────────────────────────────────
CREATE TABLE otp_tokens (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token         VARCHAR(6)  NOT NULL,
    purpose       VARCHAR(30) NOT NULL,
    expires_at    TIMESTAMP   NOT NULL,
    is_used       BOOLEAN NOT NULL DEFAULT FALSE,
    attempt_count INT     NOT NULL DEFAULT 0,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_otp_user_purpose ON otp_tokens(user_id, purpose, is_used);

-- ─── HOSPITALS ───────────────────────────────────────────────
CREATE TABLE hospitals (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(150) NOT NULL,
    address         TEXT         NOT NULL,
    state           VARCHAR(50)  NOT NULL,
    lga             VARCHAR(50)  NOT NULL,
    latitude        DECIMAL(10,8),
    longitude       DECIMAL(11,8),
    contact_email   VARCHAR(150) NOT NULL,
    contact_phone   VARCHAR(20)  NOT NULL,
    admin_user_id   UUID REFERENCES users(id),
    is_approved     BOOLEAN NOT NULL DEFAULT FALSE,
    approved_at     TIMESTAMP,
    approved_by     UUID REFERENCES users(id),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_hospitals_state       ON hospitals(state);
CREATE INDEX idx_hospitals_state_lga   ON hospitals(state, lga);
CREATE INDEX idx_hospitals_approved    ON hospitals(is_approved);

-- ─── BLOOD STOCK ─────────────────────────────────────────────
CREATE TABLE blood_stock (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hospital_id      UUID NOT NULL REFERENCES hospitals(id) ON DELETE CASCADE,
    blood_type       VARCHAR(15) NOT NULL,
    units_available  INT  NOT NULL DEFAULT 0,
    last_updated     TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by       UUID REFERENCES users(id),
    UNIQUE(hospital_id, blood_type)
);

CREATE INDEX idx_blood_stock_type ON blood_stock(blood_type, units_available);

-- ─── BLOOD REQUESTS ──────────────────────────────────────────
CREATE TABLE blood_requests (
    id                         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requesting_hospital_id     UUID NOT NULL REFERENCES hospitals(id),
    blood_type                 VARCHAR(15) NOT NULL,
    units_needed               INT  NOT NULL,
    urgency                    VARCHAR(20) NOT NULL,
    patient_name               VARCHAR(100),
    reason                     TEXT,
    status                     VARCHAR(25) NOT NULL DEFAULT 'OPEN',
    fulfilled_by_hospital_id   UUID REFERENCES hospitals(id),
    accident_report_id         UUID,
    created_at                 TIMESTAMP NOT NULL DEFAULT NOW(),
    fulfilled_at               TIMESTAMP,
    expires_at                 TIMESTAMP
);

CREATE INDEX idx_blood_requests_status   ON blood_requests(status);
CREATE INDEX idx_blood_requests_urgency  ON blood_requests(urgency, status);

-- ─── DONORS ──────────────────────────────────────────────────
CREATE TABLE donors (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id               UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    blood_type            VARCHAR(15)    NOT NULL,
    date_of_birth         DATE           NOT NULL,
    weight_kg             DECIMAL(5,2),
    state                 VARCHAR(50),
    lga                   VARCHAR(50),
    last_donation_date    DATE,
    total_donations       INT    NOT NULL DEFAULT 0,
    is_eligible           BOOLEAN NOT NULL DEFAULT TRUE,
    ineligibility_reason  TEXT,
    created_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_donors_blood_type ON donors(blood_type, is_eligible);
CREATE INDEX idx_donors_state      ON donors(state, blood_type);

-- ─── DONATIONS ───────────────────────────────────────────────
CREATE TABLE donations (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    donor_id         UUID NOT NULL REFERENCES donors(id),
    hospital_id      UUID NOT NULL REFERENCES hospitals(id),
    appointment_date TIMESTAMP,
    donation_date    TIMESTAMP,
    units_donated    DECIMAL(4,2) NOT NULL DEFAULT 1.0,
    status           VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED',
    notes            TEXT,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_donations_donor   ON donations(donor_id);
CREATE INDEX idx_donations_status  ON donations(status, appointment_date);
