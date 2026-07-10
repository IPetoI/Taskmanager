package com.ipetoi.taskmanager.controller;

import com.ipetoi.taskmanager.dto.CreateTaskRequest;
import com.ipetoi.taskmanager.dto.TaskDto;
import com.ipetoi.taskmanager.model.Task;
import com.ipetoi.taskmanager.service.TaskService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST endpoints for task management (CRUD operations and listing).
 * <p>
 * Security note: the task owner ({@code username}) is ALWAYS retrieved from the authenticated
 * {@link Authentication} object ({@code authentication.getName()}) in every endpoint. The
 * {@code JwtAuthenticationFilter} sets this value based on the JWT token. The client's request
 * body or query parameters NEVER affect which user's tasks are accessed, preventing a user from
 * retrieving, modifying, or deleting another user's tasks using their own valid token.
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<TaskDto> create(@Valid @RequestBody CreateTaskRequest req, Authentication authentication) {
        Task created = taskService.createTask(authentication.getName(), req);
        return ResponseEntity.status(201).body(TaskDto.from(created));
    }

    // GET /api/tasks - the caller's own tasks with optional status/priority/dateFrom/dateTo filters
    @GetMapping
    public ResponseEntity<List<TaskDto>> listMine(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            Authentication authentication) {

        List<Task> tasks = taskService.findTasksByUser(authentication.getName(), status, priority, dateFrom, dateTo);
        List<TaskDto> dtos = tasks.stream().map(TaskDto::from).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskDto> update(@PathVariable Long id, @Valid @RequestBody CreateTaskRequest req,
                                          Authentication authentication) {
        Task updated = taskService.updateTask(id, authentication.getName(), req);
        return ResponseEntity.ok(TaskDto.from(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        taskService.deleteTask(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}