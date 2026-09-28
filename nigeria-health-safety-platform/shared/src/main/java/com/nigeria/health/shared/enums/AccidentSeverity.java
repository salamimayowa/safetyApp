package com.nigeria.health.shared.enums;

/**
 * Severity classification for road accident reports.
 * FATAL severity triggers immediate SUPER_ADMIN email alert
 * and FRSC officer direct SMS.
 */
public enum AccidentSeverity {
    MINOR,    // fender bender, no injuries
    SERIOUS,  // injuries present, casualties need medical attention
    FATAL     // deaths involved — triggers highest priority response
}
