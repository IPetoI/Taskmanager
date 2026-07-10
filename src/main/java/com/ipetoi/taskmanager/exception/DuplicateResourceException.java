package com.ipetoi.taskmanager.exception;

/**
 * 409 Conflict: the requested resource (e.g., username) already exists.
 * {@link GlobalExceptionHandler} converts it into an HTTP 409 response.
 */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}