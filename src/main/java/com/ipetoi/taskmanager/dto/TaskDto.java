package com.ipetoi.taskmanager.dto;

import com.ipetoi.taskmanager.model.TaskPriority;
import com.ipetoi.taskmanager.model.TaskStatus;
import com.ipetoi.taskmanager.model.Task;

import java.time.LocalDateTime;

/**
 * Client-facing representation of the Task entity.
 * Instead of the owner User object, it only contains the owner's username, as the frontend
 * does not need the complete User data.
 */
public class TaskDto {
    private Long id;
    private String title;
    private String description;
    private TaskPriority priority;
    private TaskStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String username;
    private com.ipetoi.taskmanager.model.RecurrenceUnit recurrenceUnit;
    private Integer recurrenceInterval;

    public TaskDto() {
    }

    public static TaskDto from(Task t) {
        TaskDto d = new TaskDto();
        d.setId(t.getId());
        d.setTitle(t.getTitle());
        d.setDescription(t.getDescription());
        d.setPriority(t.getPriority());
        d.setStatus(t.getStatus());
        d.setStartDate(t.getStartDate());
        d.setEndDate(t.getEndDate());
        d.setUsername(t.getOwner() != null ? t.getOwner().getUsername() : null);
        d.setRecurrenceUnit(t.getRecurrenceUnit());
        d.setRecurrenceInterval(t.getRecurrenceInterval());
        return d;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public com.ipetoi.taskmanager.model.RecurrenceUnit getRecurrenceUnit() {
        return recurrenceUnit;
    }

    public void setRecurrenceUnit(com.ipetoi.taskmanager.model.RecurrenceUnit recurrenceUnit) {
        this.recurrenceUnit = recurrenceUnit;
    }

    public Integer getRecurrenceInterval() {
        return recurrenceInterval;
    }

    public void setRecurrenceInterval(Integer recurrenceInterval) {
        this.recurrenceInterval = recurrenceInterval;
    }
}