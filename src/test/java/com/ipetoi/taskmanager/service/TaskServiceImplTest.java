package com.ipetoi.taskmanager.service;

import com.ipetoi.taskmanager.dto.CreateTaskRequest;
import com.ipetoi.taskmanager.exception.ResourceNotFoundException;
import com.ipetoi.taskmanager.model.RecurrenceUnit;
import com.ipetoi.taskmanager.model.Task;
import com.ipetoi.taskmanager.model.TaskPriority;
import com.ipetoi.taskmanager.model.TaskStatus;
import com.ipetoi.taskmanager.model.User;
import com.ipetoi.taskmanager.repository.TaskRepository;
import com.ipetoi.taskmanager.service.impl.TaskServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.ipetoi.taskmanager.TestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    TaskRepository taskRepository;
    @Mock
    UserService userService;
    @InjectMocks
    TaskServiceImpl taskService;

    // --- CreateTask ---

    @Test
    void createTaskShouldPersistTaskForOwner() {
        User owner = regularUser(1L, "peto", "peto@example.com");
        when(userService.findByUsername("peto")).thenReturn(owner);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateTaskRequest req = createRequest("Test", TaskPriority.HIGH, TaskStatus.TODO);
        req.setDescription("Valami leírás.");

        Task task = taskService.createTask("peto", req);

        assertEquals("Test", task.getTitle());
        assertEquals(owner, task.getOwner());
        verify(taskRepository).save(any(Task.class));
        verify(userService).findByUsername("peto");
    }

    @Test
    void createTaskShouldRejectMissingOwner() {
        when(userService.findByUsername("ismeretlen")).thenReturn(null);

        assertThrows(ResourceNotFoundException.class,
                () -> taskService.createTask("ismeretlen", createRequest("Test", TaskPriority.LOW, TaskStatus.TODO)));

        verify(taskRepository, never()).save(any());
    }

    @Test
    void createTaskShouldRejectEndDateBeforeStartDate() {
        CreateTaskRequest req = createRequest("Test", TaskPriority.MEDIUM, TaskStatus.TODO);
        req.setStartDate(LocalDateTime.of(2026, 1, 10, 12, 0));
        req.setEndDate(LocalDateTime.of(2026, 1, 9, 12, 0));

        assertThrows(Exception.class, () -> taskService.createTask("peto", req));

        verifyNoInteractions(taskRepository);
    }

    // --- UpdateTask ---

    @Test
    void updateTaskShouldRejectMissingTask() {
        when(taskRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> taskService.updateTask(1L, "peto", createRequest("Test", TaskPriority.MEDIUM, TaskStatus.DONE)));

        verify(taskRepository, never()).save(any());
    }

    @Test
    void updateTaskShouldRejectWhenNotOwner() {
        User owner = regularUser(1L, "owner", "owner@example.com");
        Task task = task(5L, "Feladat", TaskPriority.LOW, TaskStatus.TODO, null, owner);
        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));

        assertThrows(ResourceNotFoundException.class,
                () -> taskService.updateTask(5L, "peto", createRequest("Test", TaskPriority.LOW, TaskStatus.TODO)));

        verify(taskRepository, never()).save(any());
    }

    @Test
    void updateTaskShouldPersistChangesForOwner() {
        User owner = regularUser(1L, "peto", "peto@example.com");
        Task existing = task(7L, "Régi cím", TaskPriority.LOW, TaskStatus.TODO, null, owner);
        when(taskRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task updated = taskService.updateTask(7L, "peto",
                createRequest("Új cím", TaskPriority.HIGH, TaskStatus.IN_PROGRESS));

        assertEquals("Új cím", updated.getTitle());
        assertEquals(TaskStatus.IN_PROGRESS, updated.getStatus());
        verify(taskRepository).save(existing);
    }

    // --- Recurrence tests ---

    @Test
    void updateTaskShouldCreateNextOccurrenceWhenCompletedWithDailyRecurrence() {
        User owner = regularUser(1L, "peto", "peto@example.com");
        Task existing = task(11L, "Pay bills", TaskPriority.MEDIUM, TaskStatus.IN_PROGRESS,
                LocalDateTime.of(2026, 1, 1, 9, 0), owner);
        existing.setEndDate(LocalDateTime.of(2026, 1, 1, 10, 0));
        existing.setRecurrenceUnit(RecurrenceUnit.DAILY);
        existing.setRecurrenceInterval(1);

        when(taskRepository.findById(11L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateTaskRequest req = createRequest("Pay bills", TaskPriority.MEDIUM, TaskStatus.DONE,
                LocalDateTime.of(2026, 1, 1, 9, 0));
        req.setEndDate(LocalDateTime.of(2026, 1, 1, 10, 0));
        req.setRecurrenceUnit(RecurrenceUnit.DAILY);
        req.setRecurrenceInterval(1);

        Task updated = taskService.updateTask(11L, "peto", req);

        assertEquals(TaskStatus.DONE, updated.getStatus());
        verify(taskRepository, atLeast(2)).save(any(Task.class));

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository, atLeast(2)).save(captor.capture());
        Task next = captor.getAllValues().stream()
                .filter(t -> t.getStatus() == TaskStatus.TODO && t.getStartDate() != null)
                .findFirst().orElse(null);
        assertNotNull(next, "Next occurrence should have been created");
        assertEquals(LocalDateTime.of(2026, 1, 2, 9, 0), next.getStartDate());
        assertEquals(LocalDateTime.of(2026, 1, 2, 10, 0), next.getEndDate());
    }

    @Test
    void updateTaskShouldCreateNextOccurrenceWhenCompletedWithWeeklyRecurrence() {
        User owner = regularUser(1L, "peto", "peto@example.com");
        Task existing = task(12L, "Weekly meeting", TaskPriority.MEDIUM, TaskStatus.IN_PROGRESS,
                LocalDateTime.of(2026, 1, 4, 10, 0), owner);
        existing.setEndDate(LocalDateTime.of(2026, 1, 4, 11, 0));
        existing.setRecurrenceUnit(RecurrenceUnit.WEEKLY);
        existing.setRecurrenceInterval(1);

        when(taskRepository.findById(12L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateTaskRequest req = createRequest("Weekly meeting", TaskPriority.MEDIUM, TaskStatus.DONE,
                LocalDateTime.of(2026, 1, 4, 10, 0));
        req.setEndDate(LocalDateTime.of(2026, 1, 4, 11, 0));
        req.setRecurrenceUnit(RecurrenceUnit.WEEKLY);
        req.setRecurrenceInterval(1);

        taskService.updateTask(12L, "peto", req);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository, atLeast(2)).save(captor.capture());
        Task next = captor.getAllValues().stream()
                .filter(t -> t.getStatus() == TaskStatus.TODO).findFirst().orElse(null);
        assertNotNull(next);
        assertEquals(LocalDateTime.of(2026, 1, 11, 10, 0), next.getStartDate());
        assertEquals(LocalDateTime.of(2026, 1, 11, 11, 0), next.getEndDate());
    }

    @Test
    void updateTaskShouldCreateNextOccurrenceWhenCompletedWithMonthlyRecurrence() {
        User owner = regularUser(1L, "peto", "peto@example.com");
        Task existing = task(13L, "Monthly report", TaskPriority.MEDIUM, TaskStatus.IN_PROGRESS,
                LocalDateTime.of(2026, 1, 15, 14, 0), owner);
        existing.setEndDate(LocalDateTime.of(2026, 1, 15, 15, 0));
        existing.setRecurrenceUnit(RecurrenceUnit.MONTHLY);
        existing.setRecurrenceInterval(1);

        when(taskRepository.findById(13L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateTaskRequest req = createRequest("Monthly report", TaskPriority.MEDIUM, TaskStatus.DONE,
                LocalDateTime.of(2026, 1, 15, 14, 0));
        req.setEndDate(LocalDateTime.of(2026, 1, 15, 15, 0));
        req.setRecurrenceUnit(RecurrenceUnit.MONTHLY);
        req.setRecurrenceInterval(1);

        taskService.updateTask(13L, "peto", req);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository, atLeast(2)).save(captor.capture());
        Task next = captor.getAllValues().stream()
                .filter(t -> t.getStatus() == TaskStatus.TODO).findFirst().orElse(null);
        assertNotNull(next);
        assertEquals(LocalDateTime.of(2026, 2, 15, 14, 0), next.getStartDate());
        assertEquals(LocalDateTime.of(2026, 2, 15, 15, 0), next.getEndDate());
    }

    @Test
    void updateTaskShouldCreateNextOccurrenceWhenCompletedWithYearlyRecurrence() {
        User owner = regularUser(1L, "peto", "peto@example.com");
        Task existing = task(14L, "Yearly review", TaskPriority.MEDIUM, TaskStatus.IN_PROGRESS,
                LocalDateTime.of(2026, 3, 1, 9, 0), owner);
        existing.setEndDate(LocalDateTime.of(2026, 3, 1, 10, 0));
        existing.setRecurrenceUnit(RecurrenceUnit.YEARLY);
        existing.setRecurrenceInterval(1);

        when(taskRepository.findById(14L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateTaskRequest req = createRequest("Yearly review", TaskPriority.MEDIUM, TaskStatus.DONE,
                LocalDateTime.of(2026, 3, 1, 9, 0));
        req.setEndDate(LocalDateTime.of(2026, 3, 1, 10, 0));
        req.setRecurrenceUnit(RecurrenceUnit.YEARLY);
        req.setRecurrenceInterval(1);

        taskService.updateTask(14L, "peto", req);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository, atLeast(2)).save(captor.capture());
        Task next = captor.getAllValues().stream()
                .filter(t -> t.getStatus() == TaskStatus.TODO).findFirst().orElse(null);
        assertNotNull(next);
        assertEquals(LocalDateTime.of(2027, 3, 1, 9, 0), next.getStartDate());
        assertEquals(LocalDateTime.of(2027, 3, 1, 10, 0), next.getEndDate());
    }

    @Test
    void updateTaskShouldHandleMissingStartOrEndDates() {
        User owner = regularUser(1L, "peto", "peto@example.com");

        // Case A: only endDate is provided, startDate is missing.
        Task a = task(15L, "End only", TaskPriority.LOW, TaskStatus.IN_PROGRESS, null, owner);
        a.setEndDate(LocalDateTime.of(2026, 4, 10, 12, 0));
        a.setRecurrenceUnit(RecurrenceUnit.WEEKLY);
        a.setRecurrenceInterval(1);

        when(taskRepository.findById(15L)).thenReturn(Optional.of(a));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateTaskRequest reqA = createRequest("End only", TaskPriority.LOW, TaskStatus.DONE);
        reqA.setEndDate(a.getEndDate());
        reqA.setRecurrenceUnit(RecurrenceUnit.WEEKLY);
        reqA.setRecurrenceInterval(1);

        taskService.updateTask(15L, "peto", reqA);

        ArgumentCaptor<Task> captorA = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository, atLeast(2)).save(captorA.capture());
        Task nextA = captorA.getAllValues().stream()
                .filter(t -> t.getStatus() == TaskStatus.TODO && t.getEndDate() != null)
                .findFirst().orElse(null);
        assertNotNull(nextA);
        assertNull(nextA.getStartDate());
        assertEquals(LocalDateTime.of(2026, 4, 17, 12, 0), nextA.getEndDate());

        // Case B: only startDate is provided, endDate is missing.
        Task b = task(16L, "Start only", TaskPriority.LOW, TaskStatus.IN_PROGRESS,
                LocalDateTime.of(2026, 5, 1, 8, 0), owner);
        b.setRecurrenceUnit(RecurrenceUnit.MONTHLY);
        b.setRecurrenceInterval(1);

        when(taskRepository.findById(16L)).thenReturn(Optional.of(b));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateTaskRequest reqB = createRequest("Start only", TaskPriority.LOW, TaskStatus.DONE,
                LocalDateTime.of(2026, 5, 1, 8, 0));
        reqB.setRecurrenceUnit(RecurrenceUnit.MONTHLY);
        reqB.setRecurrenceInterval(1);

        taskService.updateTask(16L, "peto", reqB);

        ArgumentCaptor<Task> captorB = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository, atLeast(2)).save(captorB.capture());
        Task nextB = captorB.getAllValues().stream()
                .filter(t -> t.getStatus() == TaskStatus.TODO && t.getStartDate() != null)
                .findFirst().orElse(null);
        assertNotNull(nextB);
        assertEquals(LocalDateTime.of(2026, 6, 1, 8, 0), nextB.getStartDate());
        assertNull(nextB.getEndDate());
    }

    // --- FindTasksByUser ---

    @Test
    void listByUserShouldReturnEmptyListWhenUserMissing() {
        when(userService.findByUsername("peto")).thenReturn(null);

        assertTrue(taskService.findTasksByUser("peto", null, null, null, null).isEmpty());
        verifyNoInteractions(taskRepository);
    }

    @Test
    void findAllTasksShouldDelegateToRepository() {
        when(taskRepository.findAll()).thenReturn(List.of(new Task(), new Task(), new Task()));

        assertEquals(3, taskService.findAllTasks().size());
        verify(taskRepository).findAll();
    }

    @Test
    void findTasksByUserWithFiltersShouldCallFilteredQuery() {
        User owner = regularUser(1L, "ujFelhasznalo", "uj@example.com");
        when(userService.findByUsername("ujFelhasznalo")).thenReturn(owner);
        when(taskRepository.findByOwnerAndOptionalFilters(eq(owner), any(), any(), isNull(), isNull()))
                .thenReturn(List.of());

        List<Task> tasks = taskService.findTasksByUser("ujFelhasznalo", "TODO", "HIGH", null, null);

        assertNotNull(tasks);
        verify(taskRepository).findByOwnerAndOptionalFilters(eq(owner), any(), any(), isNull(), isNull());
    }

    // --- DeleteTask ---

    @Test
    void deleteTaskShouldThrowWhenNotOwner() {
        User owner = regularUser(1L, "owner", "owner@example.com");
        Task t = task(5L, "Feladat", TaskPriority.LOW, TaskStatus.TODO, null, owner);
        when(taskRepository.findById(5L)).thenReturn(Optional.of(t));

        assertThrows(ResourceNotFoundException.class, () -> taskService.deleteTask(5L, "peto"));
        verify(taskRepository, never()).delete(any());
    }

    @Test
    void deleteTaskShouldRemoveTaskForOwner() {
        User owner = regularUser(1L, "peto", "peto@example.com");
        Task t = task(9L, "Feladat", TaskPriority.LOW, TaskStatus.TODO, null, owner);
        when(taskRepository.findById(9L)).thenReturn(Optional.of(t));

        taskService.deleteTask(9L, "peto");

        verify(taskRepository).delete(t);
    }

    @Test
    void deleteTaskShouldRejectMissingTask() {
        when(taskRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.deleteTask(404L, "peto"));
        verify(taskRepository, never()).delete(any());
    }

    // --- AdminUpdateTask / AdminDeleteTask ---

    @Test
    void adminUpdateTaskShouldPersistChangesRegardlessOfOwner() {
        User owner = regularUser(1L, "peto", "peto@example.com");
        Task existing = task(20L, "Régi cím", TaskPriority.LOW, TaskStatus.TODO, null, owner);
        when(taskRepository.findById(20L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task updated = taskService.adminUpdateTask(20L,
                createRequest("Admin által módosított", TaskPriority.HIGH, TaskStatus.DONE));

        assertEquals("Admin által módosított", updated.getTitle());
        assertEquals(TaskStatus.DONE, updated.getStatus());
        verify(taskRepository).save(existing);
        verifyNoInteractions(userService);
    }

    @Test
    void adminUpdateTaskShouldRejectMissingTask() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> taskService.adminUpdateTask(999L, createRequest("Test", TaskPriority.LOW, TaskStatus.TODO)));

        verify(taskRepository, never()).save(any());
    }

    @Test
    void adminDeleteTaskShouldRemoveSingleNonRecurringTask() {
        User owner = regularUser(1L, "peto", "peto@example.com");
        Task t = task(21L, "Feladat", TaskPriority.LOW, TaskStatus.TODO, null, owner);
        when(taskRepository.findById(21L)).thenReturn(Optional.of(t));

        taskService.adminDeleteTask(21L);

        verify(taskRepository).delete(t);
    }

    @Test
    void adminDeleteTaskShouldRemoveWholeRecurringSeries() {
        User owner = regularUser(1L, "peto", "peto@example.com");
        Task root = task(22L, "Heti", TaskPriority.LOW, TaskStatus.TODO, null, owner);
        root.setRecurrenceUnit(RecurrenceUnit.WEEKLY);
        Task sibling = task(23L, "Heti", TaskPriority.LOW, TaskStatus.TODO, null, owner);

        when(taskRepository.findById(22L)).thenReturn(Optional.of(root));
        when(taskRepository.findByOwnerAndRecurrenceRootIdOrderByStartDateAsc(owner, 22L))
                .thenReturn(List.of(root, sibling));

        taskService.adminDeleteTask(22L);

        verify(taskRepository).deleteAll(List.of(root, sibling));
        verify(taskRepository, never()).delete(any());
    }

    @Test
    void adminDeleteTaskShouldRejectMissingTask() {
        when(taskRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.adminDeleteTask(404L));
        verify(taskRepository, never()).delete(any());
        verify(taskRepository, never()).deleteAll(any());
    }
}