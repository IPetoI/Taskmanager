package com.ipetoi.taskmanager.model;

/**
 * Task priority level. Stored as a string in the database due to
 * {@code @Enumerated(EnumType.STRING)} ('LOW', 'MEDIUM', 'HIGH'), not as an ordinal value.
 * This prevents enum order changes from accidentally altering the meaning of existing data.
 */
public enum TaskPriority {
    LOW,
    MEDIUM,
    HIGH
}
