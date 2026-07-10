package com.ipetoi.taskmanager.service.impl;

import com.ipetoi.taskmanager.dto.CreateTaskRequest;
import com.ipetoi.taskmanager.exception.ResourceNotFoundException;
import com.ipetoi.taskmanager.model.RecurrenceUnit;
import com.ipetoi.taskmanager.model.Task;
import com.ipetoi.taskmanager.model.TaskPriority;
import com.ipetoi.taskmanager.model.TaskStatus;
import com.ipetoi.taskmanager.model.User;
import com.ipetoi.taskmanager.model.UserRole;
import com.ipetoi.taskmanager.repository.TaskRepository;
import com.ipetoi.taskmanager.service.TaskService;
import com.ipetoi.taskmanager.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final UserService userService;

    public TaskServiceImpl(TaskRepository taskRepository, UserService userService) {
        this.taskRepository = taskRepository;
        this.userService = userService;
    }

    @Override
    public Task createTask(String ownerUsername, CreateTaskRequest req) {
        validateDates(req);
        User owner = resolveOwner(ownerUsername);
        ensureTaskManagementAllowed(owner);

        Task t = new Task();
        applyRequestToTask(t, req);
        t.setOwner(owner);
        Task saved = taskRepository.save(t);

        if (saved.getRecurrenceUnit() != RecurrenceUnit.NONE && saved.getRecurrenceRootId() == null) {
            saved.setRecurrenceRootId(saved.getId());
            saved = taskRepository.save(saved);
            ensureFutureOccurrences(saved);
        }

        return saved;
    }

    @Override
    public List<Task> findTasksByUser(String username, String status, String priority,
                                      LocalDateTime dateFrom, LocalDateTime dateTo) {
        User owner = userService.findByUsername(username);
        if (owner == null) {
            return new ArrayList<>();
        }
        return taskRepository.findByOwnerAndOptionalFilters(
                owner, parseStatus(status), parsePriority(priority), dateFrom, dateTo);
    }

    @Override
    public List<Task> findAllTasks() {
        return taskRepository.findAll();
    }

    @Override
    public Task updateTask(Long id, String ownerUsername, CreateTaskRequest req) {
        validateDates(req);
        Task t = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + id));

        requireOwnership(t, ownerUsername, id);
        ensureTaskManagementAllowed(t.getOwner());

        TaskStatus previousStatus = t.getStatus();
        applyRequestToTask(t, req);
        Task saved = taskRepository.save(t);

        // If the task was just marked as DONE and is recurring, generate additional instances up to the horizon.
        if (previousStatus != TaskStatus.DONE
                && saved.getStatus() == TaskStatus.DONE
                && saved.getRecurrenceUnit() != RecurrenceUnit.NONE) {
            Long rootId = saved.getRecurrenceRootId() != null ? saved.getRecurrenceRootId() : saved.getId();
            Task root = taskRepository.findById(rootId).orElse(saved);
            ensureFutureOccurrences(root);
        }

        return saved;
    }

    @Override
    public void deleteTask(Long id, String ownerUsername) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + id));

        requireOwnership(task, ownerUsername, id);
        ensureTaskManagementAllowed(task.getOwner());
        deleteTaskOrSeries(task);
    }

    @Override
    public Task adminUpdateTask(Long id, CreateTaskRequest req) {
        validateDates(req);
        Task t = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + id));

        applyRequestToTask(t, req);
        return taskRepository.save(t);
    }

    @Override
    public void adminDeleteTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + id));
        deleteTaskOrSeries(task);
    }

    // --- Private helpers

    /**
     * Applies the fields from {@link CreateTaskRequest} to the task entity.
     * This 7-line block was previously duplicated three times
     * ({@code createTask}, {@code updateTask}, {@code adminUpdateTask}).
     */
    private void applyRequestToTask(Task t, CreateTaskRequest req) {
        t.setTitle(req.getTitle());
        t.setDescription(req.getDescription());
        t.setPriority(req.getPriority());
        t.setStatus(req.getStatus());
        t.setStartDate(req.getStartDate());
        t.setEndDate(req.getEndDate());
        t.setRecurrenceUnit(req.getRecurrenceUnit() == null ? RecurrenceUnit.NONE : req.getRecurrenceUnit());
        t.setRecurrenceInterval(req.getRecurrenceInterval());
    }

    /**
     * Deletes the entire recurring series for recurring tasks; for one-time tasks, deletes only
     * the task itself. This logic was previously duplicated in {@code deleteTask} and
     * {@code adminDeleteTask}.
     */
    private void deleteTaskOrSeries(Task task) {
        if (task.getRecurrenceUnit() != RecurrenceUnit.NONE || task.getRecurrenceRootId() != null) {
            Long rootId = task.getRecurrenceRootId() != null ? task.getRecurrenceRootId() : task.getId();
            List<Task> series = taskRepository.findByOwnerAndRecurrenceRootIdOrderByStartDateAsc(task.getOwner(), rootId);
            if (!series.isEmpty()) {
                taskRepository.deleteAll(series);
                return;
            }
        }
        taskRepository.delete(task);
    }

    private User resolveOwner(String username) {
        User owner = userService.findByUsername(username);
        if (owner == null) {
            throw new ResourceNotFoundException("User not found: " + username);
        }
        return owner;
    }

    /**
     * Verifies that the authenticated user is the actual owner of the task.
     * If not, throws a 404 response (not 403!) so the client cannot distinguish between
     * "this task does not exist" and "this task exists but does not belong to you".
     */
    private void requireOwnership(Task task, String requesterUsername, Long taskId) {
        if (!task.getOwner().getUsername().equals(requesterUsername)) {
            throw new ResourceNotFoundException("Task not found: " + taskId);
        }
    }

    private void ensureTaskManagementAllowed(User user) {
        if (user != null && user.getRole() == UserRole.ADMIN) {
            throw new AccessDeniedException("Admins cannot manage tasks");
        }
    }

    private TaskStatus parseStatus(String value) {
        return value == null || value.isBlank() ? null : TaskStatus.valueOf(value.trim().toUpperCase());
    }

    private TaskPriority parsePriority(String value) {
        return value == null || value.isBlank() ? null : TaskPriority.valueOf(value.trim().toUpperCase());
    }

    private void validateDates(CreateTaskRequest req) {
        if (req.getStartDate() != null && req.getEndDate() != null
                && req.getEndDate().isBefore(req.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date cannot be before start date");
        }
    }

    /**
     * Ensures that recurring task instances always exist in the database up to the configured
     * horizon. The horizon and interval calculation logic are defined in the
     * {@link RecurrenceUnit} enum, not here, keeping this method free of switch statements.
     */
    private void ensureFutureOccurrences(Task root) {
        if (root == null) {
            return;
        }

        RecurrenceUnit unit = root.getRecurrenceUnit();
        LocalDateTime seriesAnchor = root.getStartDate() != null ? root.getStartDate() : root.getEndDate();
        if (seriesAnchor == null) {
            return;
        }

        LocalDateTime horizon = unit.horizon(seriesAnchor);
        if (horizon == null) {
            return;
        }

        int interval = root.getRecurrenceInterval() == null || root.getRecurrenceInterval() <= 0
                ? 1 : root.getRecurrenceInterval();
        boolean useStartDate = root.getStartDate() != null;

        Long rootId = root.getRecurrenceRootId() != null ? root.getRecurrenceRootId() : root.getId();
        List<Task> existing = taskRepository.findByOwnerAndRecurrenceRootIdOrderByStartDateAsc(root.getOwner(), rootId);

        LocalDateTime lastAnchor = existing.stream()
                .map(t -> useStartDate ? t.getStartDate() : t.getEndDate())
                .filter(d -> d != null)
                .max(LocalDateTime::compareTo)
                .orElse(seriesAnchor);

        if (lastAnchor.isAfter(horizon)) {
            return;
        }

        LocalDateTime nextAnchor = unit.advance(lastAnchor, interval);
        LocalDateTime nextStart = root.getStartDate() != null ? unit.advance(root.getStartDate(), interval) : null;
        LocalDateTime nextEnd = root.getEndDate() != null ? unit.advance(root.getEndDate(), interval) : null;

        while (nextAnchor != null && !nextAnchor.isAfter(horizon)) {
            Task next = new Task();
            next.setTitle(root.getTitle());
            next.setDescription(root.getDescription());
            next.setPriority(root.getPriority());
            next.setStatus(TaskStatus.TODO);
            next.setStartDate(useStartDate ? nextStart : null);
            next.setEndDate(nextEnd);
            next.setOwner(root.getOwner());
            next.setRecurrenceUnit(unit);
            next.setRecurrenceInterval(root.getRecurrenceInterval());
            next.setRecurrenceRootId(rootId);
            taskRepository.save(next);

            nextAnchor = unit.advance(nextAnchor, interval);
            if (nextStart != null) nextStart = unit.advance(nextStart, interval);
            if (nextEnd != null) nextEnd = unit.advance(nextEnd, interval);
        }
    }
}