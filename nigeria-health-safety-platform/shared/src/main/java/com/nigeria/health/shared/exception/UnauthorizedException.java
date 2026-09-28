package com.nigeria.health.shared.exception;

/**
 * Thrown when a user attempts an action they don't have permission for.
 * GlobalExceptionHandler maps this to HTTP 403.
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
