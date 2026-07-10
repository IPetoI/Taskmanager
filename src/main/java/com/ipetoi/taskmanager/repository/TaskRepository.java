package com.ipetoi.taskmanager.repository;

import com.ipetoi.taskmanager.model.Task;
import com.ipetoi.taskmanager.model.TaskPriority;
import com.ipetoi.taskmanager.model.TaskStatus;
import com.ipetoi.taskmanager.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByOwner(User owner);

    List<Task> findByOwnerAndRecurrenceRootIdOrderByStartDateAsc(User owner, Long recurrenceRootId);

    /**
     * Returns the owner's tasks, optionally filtered by status, priority, and date range.
     * All filter parameters can be null, in which case the corresponding condition is ignored
     * (using the `:param IS NULL OR ...` pattern). Date filtering checks the task's actual
     * "activity" window: COALESCE(endDate, startDate) is compared against the lower bound,
     * and startDate is compared against the upper bound.
     */
    @Query("SELECT t FROM Task t WHERE t.owner = :owner"
            + " AND (:status IS NULL OR t.status = :status)"
            + " AND (:priority IS NULL OR t.priority = :priority)"
            + " AND (:dateFrom IS NULL OR COALESCE(t.endDate, t.startDate) >= :dateFrom)"
            + " AND (:dateTo IS NULL OR t.startDate <= :dateTo)")
    List<Task> findByOwnerAndOptionalFilters(@Param("owner") User owner,
                                             @Param("status") TaskStatus status,
                                             @Param("priority") TaskPriority priority,
                                             @Param("dateFrom") LocalDateTime dateFrom,
                                             @Param("dateTo") LocalDateTime dateTo);
}