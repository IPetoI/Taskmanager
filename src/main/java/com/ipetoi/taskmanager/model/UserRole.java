package com.ipetoi.taskmanager.model;

/**
 * User role. Stored as a string in the database due to {@code @Enumerated(EnumType.STRING)}
 * ('USER', 'ADMIN').
 * <p>
 * Exactly one ADMIN account exists in the application; this is ensured by the
 * {@code ApplicationConfig.adminSeeder} that runs during application startup.
 */
public enum UserRole {
    USER,
    ADMIN
}
