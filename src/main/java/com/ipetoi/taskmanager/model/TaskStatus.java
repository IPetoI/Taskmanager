package com.ipetoi.taskmanager.model;

/**
 * Task lifecycle status: TODO -> IN_PROGRESS -> DONE.
 * Stored as a string in the database due to {@code @Enumerated(EnumType.STRING)}.
 */
public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE
}
