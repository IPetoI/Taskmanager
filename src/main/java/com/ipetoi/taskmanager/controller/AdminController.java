package com.ipetoi.taskmanager.controller;

import com.ipetoi.taskmanager.dto.AdminUserUpdateRequest;
import com.ipetoi.taskmanager.dto.CreateTaskRequest;
import com.ipetoi.taskmanager.dto.TaskDto;
import com.ipetoi.taskmanager.dto.UserDto;
import com.ipetoi.taskmanager.model.Task;
import com.ipetoi.taskmanager.model.User;
import com.ipetoi.taskmanager.service.TaskService;
import com.ipetoi.taskmanager.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin endpoints for listing, editing, and deleting users and tasks.
 * Accessible only by the single ADMIN account (see {@link com.ipetoi.taskmanager.config.SecurityConfig}).
 * The admin account cannot edit or delete itself here - it is excluded from the user list,
 * which only contains regular users.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;
    private final TaskService taskService;

    public AdminController(UserService userService, TaskService taskService) {
        this.userService = userService;
        this.taskService = taskService;
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> listUsers() {
        List<UserDto> users = userService.findAllNonAdminUsers().stream()
                .map(UserDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id,
                                              @Valid @RequestBody AdminUserUpdateRequest req) {
        User updated = userService.updateUser(id, req.getUsername(), req.getEmail(), req.getPassword());
        return ResponseEntity.ok(UserDto.from(updated));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tasks")
    public ResponseEntity<List<TaskDto>> listTasks() {
        List<TaskDto> tasks = taskService.findAllTasks().stream()
                .map(TaskDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(tasks);
    }

    @PutMapping("/tasks/{id}")
    public ResponseEntity<TaskDto> updateTask(@PathVariable Long id,
                                              @Valid @RequestBody CreateTaskRequest req) {
        Task updated = taskService.adminUpdateTask(id, req);
        return ResponseEntity.ok(TaskDto.from(updated));
    }

    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.adminDeleteTask(id);
        return ResponseEntity.noContent().build();
    }
}