package com.nigeria.health.shared.enums;

/**
 * All user roles across the Nigeria Health & Safety Platform.
 * Each role controls what endpoints a user can access.
 */
public enum Role {
    SUPER_ADMIN,       // full access to everything
    HOSPITAL_ADMIN,    // manages hospital blood stock, receives accident alerts
    PHARMACY_ADMIN,    // registers drugs for NAFDAC verification
    FRSC_OFFICER,      // receives and manages accident reports
    DONOR,             // registers, books donation appointments
    CITIZEN            // reports accidents, verifies drugs, searches for blood
}
