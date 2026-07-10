package com.ipetoi.taskmanager;

import com.ipetoi.taskmanager.dto.CreateTaskRequest;
import com.ipetoi.taskmanager.model.RecurrenceUnit;
import com.ipetoi.taskmanager.model.Task;
import com.ipetoi.taskmanager.model.TaskPriority;
import com.ipetoi.taskmanager.model.TaskStatus;
import com.ipetoi.taskmanager.model.User;
import com.ipetoi.taskmanager.model.UserRole;

import java.time.LocalDateTime;

/**
 * Shared test utility objects.
 * Used by AdminControllerTest, TaskControllerTest, TaskServiceImplTest, and UserServiceImplTest.
 */
public final class TestFixtures {

    private TestFixtures() {
    }

    public static User regularUser(Long id, String username, String email) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setEmail(email);
        u.setPassword("hashed");
        u.setRole(UserRole.USER);
        return u;
    }

    public static User adminUser(Long id) {
        User u = new User();
        u.setId(id);
        u.setUsername("admin");
        u.setEmail("petoojozsef@gmail.com");
        u.setPassword("hashed-admin");
        u.setRole(UserRole.ADMIN);
        return u;
    }

    public static Task task(Long id, String title, TaskPriority priority, TaskStatus status,
                            LocalDateTime start, User owner) {
        Task t = new Task();
        t.setId(id);
        t.setTitle(title);
        t.setPriority(priority);
        t.setStatus(status);
        t.setStartDate(start);
        t.setOwner(owner);
        t.setRecurrenceUnit(RecurrenceUnit.NONE);
        return t;
    }

    public static CreateTaskRequest createRequest(String title, TaskPriority priority,
                                                  TaskStatus status) {
        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle(title);
        req.setPriority(priority);
        req.setStatus(status);
        return req;
    }

    public static CreateTaskRequest createRequest(String title, TaskPriority priority,
                                                  TaskStatus status, LocalDateTime start) {
        CreateTaskRequest req = createRequest(title, priority, status);
        req.setStartDate(start);
        return req;
    }
}