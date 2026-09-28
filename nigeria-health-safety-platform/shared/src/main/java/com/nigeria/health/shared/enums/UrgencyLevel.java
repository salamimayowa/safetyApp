package com.nigeria.health.shared.enums;

/**
 * Urgency levels for blood requests.
 * CRITICAL requests trigger immediate multi-hospital alerts.
 * URGENT requests escalate after 2 hours if unfulfilled.
 * ROUTINE requests are standard and can wait.
 */
public enum UrgencyLevel {
    ROUTINE,   // standard request, can wait
    URGENT,    // needed within a few hours
    CRITICAL   // life-threatening, triggers immediate broadcast to all state hospitals
}
