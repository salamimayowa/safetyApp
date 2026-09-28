-- ============================================================
-- V1__accident_schema.sql
-- Accident Report Module Schema
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE frsc_stations (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                VARCHAR(150) NOT NULL,
    state               VARCHAR(50)  NOT NULL,
    lga                 VARCHAR(50)  NOT NULL,
    latitude            DECIMAL(10,8),
    longitude           DECIMAL(11,8),
    contact_email       VARCHAR(150),
    contact_phone       VARCHAR(20),
    officer_in_charge   VARCHAR(100),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_frsc_state_lga ON frsc_stations(state, lga);

CREATE TABLE accident_reports (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_code              VARCHAR(20) NOT NULL UNIQUE,
    reported_by_user_id         UUID,
    reporter_phone              VARCHAR(20),
    latitude                    DECIMAL(10,8) NOT NULL,
    longitude                   DECIMAL(11,8) NOT NULL,
    state                       VARCHAR(50)   NOT NULL,
    lga                         VARCHAR(50)   NOT NULL,
    landmark                    TEXT,
    severity                    VARCHAR(20)   NOT NULL,
    number_of_casualties        INT NOT NULL DEFAULT 0,
    number_of_vehicles          INT NOT NULL DEFAULT 1,
    description                 TEXT,
    blood_type_needed           VARCHAR(15),
    nearest_frsc_station_id     UUID REFERENCES frsc_stations(id),
    assigned_officer_id         UUID,
    hospital_notified_id        UUID,
    blood_request_id            UUID,
    status                      VARCHAR(30) NOT NULL DEFAULT 'REPORTED',
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
    acknowledged_at             TIMESTAMP,
    resolved_at                 TIMESTAMP
);

CREATE INDEX idx_accident_status   ON accident_reports(status);
CREATE INDEX idx_accident_state    ON accident_reports(state);
CREATE INDEX idx_accident_severity ON accident_reports(severity, status);

CREATE TABLE accident_photos (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    accident_report_id  UUID NOT NULL REFERENCES accident_reports(id) ON DELETE CASCADE,
    photo_url           TEXT NOT NULL,
    uploaded_at         TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE accident_updates (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    accident_report_id  UUID NOT NULL REFERENCES accident_reports(id) ON DELETE CASCADE,
    updated_by_user_id  UUID,
    message             TEXT NOT NULL,
    status_changed_to   VARCHAR(30),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ─── FRSC Station Seed Data ───────────────────────────────────
INSERT INTO frsc_stations (name, state, lga, contact_email, contact_phone, officer_in_charge) VALUES
    ('FRSC Lagos Command — Ikeja',       'Lagos',  'Ikeja',       'frsc.lagos@frsc.gov.ng',   '07080396615', 'Sector Commander Adeyemi'),
    ('FRSC Lagos Command — Surulere',    'Lagos',  'Surulere',    'frsc.surulere@frsc.gov.ng','07080396616', 'Unit Commander Badmus'),
    ('FRSC Abuja Command — Municipal',   'Abuja',  'Municipal',   'frsc.abuja@frsc.gov.ng',  '07080396617', 'Sector Commander Musa'),
    ('FRSC Kano Command — Nassarawa',    'Kano',   'Nassarawa',   'frsc.kano@frsc.gov.ng',   '07080396618', 'Sector Commander Garba'),
    ('FRSC Enugu Command — Enugu North', 'Enugu',  'Enugu North', 'frsc.enugu@frsc.gov.ng',  '07080396619', 'Sector Commander Okafor'),
    ('FRSC Rivers Command — Port Harcourt City', 'Rivers', 'Port Harcourt City', 'frsc.rivers@frsc.gov.ng', '07080396620', 'Sector Commander Tamuno');
