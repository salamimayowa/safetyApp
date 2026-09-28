package com.nigeria.health.shared.exception;

/**
 * Thrown when a requested resource does not exist in the database.
 * GlobalExceptionHandler maps this to HTTP 404.
 *
 * Usage:
 *   Hospital hospital = hospitalRepository.findById(id)
 *       .orElseThrow(() -> new ResourceNotFoundException("Hospital not found with id: " + id));
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
