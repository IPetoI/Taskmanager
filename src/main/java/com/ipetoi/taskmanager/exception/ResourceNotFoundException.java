package com.ipetoi.taskmanager.exception;

/**
 * 404 Not Found: the requested resource (e.g., Task, User) does not exist - or exists but
 * does not belong to the requesting user (see TaskServiceImpl: when accessing another
 * user's task, this exception is intentionally thrown instead of returning 403, so the
 * client cannot distinguish between "does not exist" and "does not belong to you").
 * {@link GlobalExceptionHandler} converts it into an HTTP 404 response.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
