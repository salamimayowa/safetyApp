package com.nigeria.health.shared.exception;

/**
 * Thrown when the request is syntactically valid but logically wrong.
 * Examples: donor not eligible, OTP expired, blood type mismatch.
 * GlobalExceptionHandler maps this to HTTP 400.
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
