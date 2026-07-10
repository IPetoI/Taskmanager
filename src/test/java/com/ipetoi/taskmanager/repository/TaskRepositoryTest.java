package com.ipetoi.taskmanager.repository;

import com.ipetoi.taskmanager.model.Task;
import com.ipetoi.taskmanager.model.TaskPriority;
import com.ipetoi.taskmanager.model.TaskStatus;
import com.ipetoi.taskmanager.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Verifies that TaskRepository JPQL queries are translated into actual SQL and executed
 * against the H2 in-memory database.
 * These queries cannot be tested with Mockito because the repository itself would be mocked.
 */
@DataJpaTest
class TaskRepositoryTest {

    @Autowired
    TestEntityManager entityManager;
    @Autowired
    TaskRepository taskRepository;

    private User peto;
    private User masik;

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 6, 1, 10, 0);

    @BeforeEach
    void setUp() {
        peto = new User();
        peto.setUsername("peto");
        peto.setEmail("peto@gmail.com");
        peto.setPassword("hash");
        entityManager.persist(peto);

        masik = new User();
        masik.setUsername("masik");
        masik.setEmail("masik@gmail.com");
        masik.setPassword("hash");
        entityManager.persist(masik);
    }

    private Task createTask(User owner, String title, TaskPriority priority, TaskStatus status,
                            LocalDateTime start, LocalDateTime end) {
        Task t = new Task();
        t.setOwner(owner);
        t.setTitle(title);
        t.setPriority(priority);
        t.setStatus(status);
        t.setStartDate(start);
        t.setEndDate(end);
        return entityManager.persist(t);
    }

    @Test
    void findByOwnerShouldReturnOnlyOwnersTasks() {
        createTask(peto, "Pető feladata", TaskPriority.HIGH, TaskStatus.TODO, BASE, null);
        createTask(masik, "Másik feladata", TaskPriority.LOW, TaskStatus.DONE, BASE, null);
        entityManager.flush();

        List<Task> result = taskRepository.findByOwner(peto);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Pető feladata");
    }

    @Test
    void findByOwnerAndOptionalFiltersShouldReturnAllWhenFiltersAreNull() {
        createTask(peto, "A", TaskPriority.HIGH, TaskStatus.TODO, BASE, null);
        createTask(peto, "B", TaskPriority.LOW, TaskStatus.DONE, BASE, null);
        entityManager.flush();

        List<Task> result = taskRepository.findByOwnerAndOptionalFilters(peto, null, null, null, null);

        assertThat(result).hasSize(2);
    }

    @Test
    void findByOwnerAndOptionalFiltersShouldFilterByStatus() {
        createTask(peto, "TODO task", TaskPriority.MEDIUM, TaskStatus.TODO, BASE, null);
        createTask(peto, "DONE task", TaskPriority.MEDIUM, TaskStatus.DONE, BASE, null);
        entityManager.flush();

        List<Task> result = taskRepository.findByOwnerAndOptionalFilters(
                peto, TaskStatus.TODO, null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("TODO task");
    }

    @Test
    void findByOwnerAndOptionalFiltersShouldFilterByPriority() {
        createTask(peto, "High task", TaskPriority.HIGH, TaskStatus.TODO, BASE, null);
        createTask(peto, "Low task", TaskPriority.LOW, TaskStatus.TODO, BASE, null);
        entityManager.flush();

        List<Task> result = taskRepository.findByOwnerAndOptionalFilters(
                peto, null, TaskPriority.HIGH, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("High task");
    }

    @Test
    void findByOwnerAndOptionalFiltersShouldFilterByBothStatusAndPriority() {
        createTask(peto, "Match", TaskPriority.HIGH, TaskStatus.IN_PROGRESS, BASE, null);
        createTask(peto, "Wrong priority", TaskPriority.LOW, TaskStatus.IN_PROGRESS, BASE, null);
        createTask(peto, "Wrong status", TaskPriority.HIGH, TaskStatus.DONE, BASE, null);
        entityManager.flush();

        List<Task> result = taskRepository.findByOwnerAndOptionalFilters(
                peto, TaskStatus.IN_PROGRESS, TaskPriority.HIGH, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Match");
    }

    @Test
    void findByOwnerAndOptionalFiltersShouldFilterByDateFrom() {
        // Task 1: startDate = BASE (2026-06-01 10:00), no endDate
        createTask(peto, "Régi", TaskPriority.LOW, TaskStatus.TODO,
                BASE, null);
        // Task 2: startDate = BASE+2days, endDate = BASE+3days
        createTask(peto, "Új", TaskPriority.LOW, TaskStatus.TODO,
                BASE.plusDays(2), BASE.plusDays(3));
        entityManager.flush();

        // dateFrom = BASE + 1 day -> "Old" is excluded (endDate=null, so COALESCE uses startDate=BASE < dateFrom).
        List<Task> result = taskRepository.findByOwnerAndOptionalFilters(
                peto, null, null, BASE.plusDays(1), null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Új");
    }

    @Test
    void findByOwnerAndOptionalFiltersShouldFilterByDateTo() {
        createTask(peto, "Korai", TaskPriority.LOW, TaskStatus.TODO, BASE, BASE.plusHours(2));
        createTask(peto, "Késői", TaskPriority.LOW, TaskStatus.TODO, BASE.plusDays(5), null);
        entityManager.flush();

        // dateTo = BASE + 1 day -> "Late" is filtered out (startDate = BASE + 5 days > dateTo).
        List<Task> result = taskRepository.findByOwnerAndOptionalFilters(
                peto, null, null, null, BASE.plusDays(1));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Korai");
    }

    @Test
    void findByOwnerAndOptionalFiltersShouldUseEndDateForDateFromWhenEndDatePresent() {
        // COALESCE(endDate, startDate): uses endDate for the dateFrom comparison when available.
        // Task endDate = BASE + 5 days, startDate = BASE - 1 day.
        createTask(peto, "Hosszú feladat", TaskPriority.HIGH, TaskStatus.IN_PROGRESS,
                BASE.minusDays(1), BASE.plusDays(5));
        entityManager.flush();

        // dateFrom = BASE + 2 days: endDate (BASE + 5 days) >= dateFrom -> remains included.
        List<Task> withDateFrom = taskRepository.findByOwnerAndOptionalFilters(
                peto, null, null, BASE.plusDays(2), null);
        assertThat(withDateFrom).hasSize(1);

        // dateFrom = BASE + 6 days: endDate (BASE + 5 days) < dateFrom -> task is filtered out.
        List<Task> tooLate = taskRepository.findByOwnerAndOptionalFilters(
                peto, null, null, BASE.plusDays(6), null);
        assertThat(tooLate).isEmpty();
    }

    @Test
    void findByOwnerAndOptionalFiltersShouldNotReturnOtherUsersTasks() {
        createTask(peto, "Pető feladata", TaskPriority.HIGH, TaskStatus.TODO, BASE, null);
        createTask(masik, "Másik feladata", TaskPriority.HIGH, TaskStatus.TODO, BASE, null);
        entityManager.flush();

        List<Task> result = taskRepository.findByOwnerAndOptionalFilters(
                peto, TaskStatus.TODO, TaskPriority.HIGH, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOwner().getUsername()).isEqualTo("peto");
    }
}