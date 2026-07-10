package com.ipetoi.taskmanager.service;

import com.ipetoi.taskmanager.dto.CreateTaskRequest;
import com.ipetoi.taskmanager.model.Task;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Every method expects the caller's authenticated username (from the JWT, passed by the
 * controller) as a parameter. This value never comes from the client's request body or
 * query parameters.
 */
public interface TaskService {

    Task createTask(String ownerUsername, CreateTaskRequest req);

    List<Task> findTasksByUser(String username, String status, String priority,
                               LocalDateTime dateFrom, LocalDateTime dateTo);

    List<Task> findAllTasks();

    Task updateTask(Long id, String ownerUsername, CreateTaskRequest req);

    void deleteTask(Long id, String ownerUsername);

    // Admin edit: skips ownership checks, allowing updates to tasks owned by any user.
    Task adminUpdateTask(Long id, CreateTaskRequest req);

    // Admin deletion: no ownership check is performed; can delete tasks belonging to any user.
    void adminDeleteTask(Long id);
}