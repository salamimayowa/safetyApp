-- ============================================================
-- V1__drug_schema.sql
-- Drug Verification Module Schema + Seed Data
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE drugs (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    brand_name            VARCHAR(150) NOT NULL,
    generic_name          VARCHAR(150) NOT NULL,
    nafdac_number         VARCHAR(50)  NOT NULL UNIQUE,
    manufacturer          VARCHAR(150) NOT NULL,
    country_of_origin     VARCHAR(100),
    category              VARCHAR(50),
    registration_date     DATE,
    expiry_date           DATE,
    dosage_form           VARCHAR(50),
    storage_instructions  TEXT,
    is_approved           BOOLEAN NOT NULL DEFAULT FALSE,
    added_by              UUID,
    approved_by           UUID,
    approved_at           TIMESTAMP,
    created_at            TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_drugs_nafdac    ON drugs(nafdac_number);
CREATE INDEX idx_drugs_approved  ON drugs(is_approved);

CREATE TABLE verification_logs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nafdac_number       VARCHAR(50) NOT NULL,
    verified_by_user_id UUID,
    result              VARCHAR(20) NOT NULL,
    state               VARCHAR(50),
    lga                 VARCHAR(50),
    latitude            DECIMAL(10,8),
    longitude           DECIMAL(11,8),
    device_info         VARCHAR(200),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_verif_nafdac     ON verification_logs(nafdac_number);
CREATE INDEX idx_verif_result_lga ON verification_logs(result, lga, created_at);

CREATE TABLE counterfeit_reports (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nafdac_number       VARCHAR(50),
    drug_id             UUID REFERENCES drugs(id),
    reported_by_user_id UUID,
    pharmacy_name       VARCHAR(150),
    seller_address      TEXT,
    state               VARCHAR(50),
    lga                 VARCHAR(50),
    description         TEXT,
    evidence_photo_url  TEXT,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    investigated_by     UUID,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    resolved_at         TIMESTAMP
);

CREATE INDEX idx_counterfeit_nafdac ON counterfeit_reports(nafdac_number);
CREATE INDEX idx_counterfeit_status ON counterfeit_reports(status);

-- ─── SEED: 10 approved Nigerian drugs ────────────────────────
INSERT INTO drugs (brand_name, generic_name, nafdac_number, manufacturer, country_of_origin,
                   category, registration_date, expiry_date, dosage_form, is_approved, approved_at)
VALUES
    ('Coartem', 'Artemether/Lumefantrine', 'A4-0141L', 'Novartis Pharma AG',
     'Switzerland', 'ANTIMALARIAL', '2020-01-01', '2027-12-31', 'Tablet', TRUE, NOW()),

    ('Amoxil', 'Amoxicillin', 'A4-1234A', 'GlaxoSmithKline',
     'United Kingdom', 'ANTIBIOTIC', '2019-06-01', '2026-06-30', 'Capsule', TRUE, NOW()),

    ('Paracetamol BP', 'Paracetamol', 'A1-0020P', 'May & Baker Nigeria Plc',
     'Nigeria', 'ANALGESIC', '2018-03-15', '2026-03-14', 'Tablet', TRUE, NOW()),

    ('Amlodipine 5mg', 'Amlodipine Besylate', 'A4-9876H', 'Pfizer Nigeria Ltd',
     'Nigeria', 'ANTIHYPERTENSIVE', '2021-07-01', '2028-06-30', 'Tablet', TRUE, NOW()),

    ('Flagyl', 'Metronidazole', 'A4-0056M', 'Sanofi-Aventis Nigeria',
     'France', 'ANTIBIOTIC', '2019-11-10', '2025-11-09', 'Tablet', TRUE, NOW()),

    ('Ciprofloxacin 500mg', 'Ciprofloxacin Hydrochloride', 'A4-3344C', 'Emzor Pharmaceuticals',
     'Nigeria', 'ANTIBIOTIC', '2020-04-01', '2027-03-31', 'Tablet', TRUE, NOW()),

    ('Lonart DS', 'Artemether/Lumefantrine', 'A4-8821L', 'Bliss GVS Pharma',
     'India', 'ANTIMALARIAL', '2020-09-01', '2026-08-31', 'Tablet', TRUE, NOW()),

    ('Vitamin C 500mg', 'Ascorbic Acid', 'A1-0099V', 'Fidson Healthcare Plc',
     'Nigeria', 'SUPPLEMENT', '2021-01-15', '2028-01-14', 'Tablet', TRUE, NOW()),

    ('ORS', 'Oral Rehydration Salts', 'A1-0011O', 'WHO/UNICEF Standard',
     'Nigeria', 'OTHER', '2019-01-01', '2026-12-31', 'Powder', TRUE, NOW()),

    ('Tenofovir 300mg', 'Tenofovir Disoproxil Fumarate', 'A4-7723T', 'Cipla Ltd',
     'India', 'ANTIRETROVIRAL', '2020-06-01', '2027-05-31', 'Tablet', TRUE, NOW());
