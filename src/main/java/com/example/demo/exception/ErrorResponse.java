package com.example.demo.exception;

/**
 * Error payload returned by API exception handlers.
 *
 * @param message human-readable error message
 * @param code machine-readable error code
 */
public record ErrorResponse(String message, String code) {
}
