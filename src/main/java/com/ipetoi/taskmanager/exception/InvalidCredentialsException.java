package com.ipetoi.taskmanager.exception;

/**
 * 401 Unauthorized: invalid login credentials.
 * The same message is intentionally returned for both "user not found" and "incorrect
 * password" cases, preventing attackers from discovering which usernames exist in the system.
 * {@link GlobalExceptionHandler} converts it into an HTTP 401 response.
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}