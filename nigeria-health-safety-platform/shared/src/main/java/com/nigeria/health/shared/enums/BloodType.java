package com.nigeria.health.shared.enums;

/**
 * ABO + Rh blood type system.
 * Used across blood stock, blood requests, and donor profiles.
 */
public enum BloodType {
    A_POSITIVE("A+"),
    A_NEGATIVE("A-"),
    B_POSITIVE("B+"),
    B_NEGATIVE("B-"),
    O_POSITIVE("O+"),
    O_NEGATIVE("O-"),
    AB_POSITIVE("AB+"),
    AB_NEGATIVE("AB-");

    private final String display;

    BloodType(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }
}
