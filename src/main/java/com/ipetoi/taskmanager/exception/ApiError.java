package com.ipetoi.taskmanager.exception;

import java.time.Instant;

/**
 * Unified, structured error response for all API errors.
 * Example JSON: {"timestamp": "...", "status": 404, "error": "Not Found",
 * "message": "Task not found: 999", "path": "/api/tasks/999"}
 */
public class ApiError {
    private final Instant timestamp = Instant.now();
    private final int status;
    private final String error;
    private final String message;
    private final String path;

    public ApiError(int status, String error, String message, String path) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }
}