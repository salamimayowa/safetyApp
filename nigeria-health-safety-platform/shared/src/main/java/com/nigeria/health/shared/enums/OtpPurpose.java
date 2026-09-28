package com.nigeria.health.shared.enums;

/**
 * Identifies what an OTP token was generated for.
 * Prevents an OTP meant for email verification
 * from being used for password reset (and vice versa).
 */
public enum OtpPurpose {
    EMAIL_VERIFICATION,
    PASSWORD_RESET,
    LOGIN_2FA
}
