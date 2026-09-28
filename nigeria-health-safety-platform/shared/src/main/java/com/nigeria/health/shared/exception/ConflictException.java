package com.nigeria.health.shared.exception;

/**
 * Thrown when creating a resource that already exists.
 * Examples: email already registered, NAFDAC number already in DB,
 * donor already registered for this user account.
 * GlobalExceptionHandler maps this to HTTP 409.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
