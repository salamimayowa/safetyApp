package com.nigeria.health.shared.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Standard API response wrapper used by every endpoint in the platform.
 *
 * Every response — success or failure — uses this shape:
 * {
 *   "success": true,
 *   "message": "Operation successful",
 *   "data": { ... },
 *   "timestamp": "2025-01-01T00:00:00"
 * }
 *
 * Use ApiResponse.success() for successful responses.
 * Use ApiResponse.error() for error responses.
 * The GlobalExceptionHandler uses this for all error cases.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    // ─── factory methods ────────────────────────────────────────────

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(String message, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
