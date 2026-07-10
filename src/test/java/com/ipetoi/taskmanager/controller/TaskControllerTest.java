package com.ipetoi.taskmanager.controller;

import com.ipetoi.taskmanager.TestFixtures;
import com.ipetoi.taskmanager.dto.CreateTaskRequest;
import com.ipetoi.taskmanager.exception.ResourceNotFoundException;
import com.ipetoi.taskmanager.model.Task;
import com.ipetoi.taskmanager.model.TaskPriority;
import com.ipetoi.taskmanager.model.TaskStatus;
import com.ipetoi.taskmanager.model.User;
import com.ipetoi.taskmanager.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @MockBean
    TaskService taskService;

    private Task sampleTask;

    @BeforeEach
    void setUp() {
        User owner = TestFixtures.regularUser(null, "peto", "peto@example.com");
        sampleTask = TestFixtures.task(1L, "Teszt feladat", TaskPriority.MEDIUM,
                TaskStatus.TODO, LocalDateTime.now(), owner);
    }

    // --- GET /api/tasks ---

    @Test
    @WithMockUser(username = "peto")
    void listMineShouldReturnTasksForAuthenticatedUser() throws Exception {
        when(taskService.findTasksByUser(eq("peto"), any(), any(), any(), any()))
                .thenReturn(List.of(sampleTask));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Teszt feladat"))
                .andExpect(jsonPath("$[0].username").value("peto"));

        // Proves the IDOR fix: the service is called with the "peto" username from the JWT,
        // not with a value provided by the client.
        verify(taskService).findTasksByUser(eq("peto"), isNull(), isNull(), isNull(), isNull());
    }

    @Test
    void listMineShouldReturn401WithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "peto")
    void listMineShouldPassFiltersToService() throws Exception {
        when(taskService.findTasksByUser(eq("peto"), eq("TODO"), eq("HIGH"), any(), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/tasks")
                        .param("status", "TODO")
                        .param("priority", "HIGH"))
                .andExpect(status().isOk());

        verify(taskService).findTasksByUser("peto", "TODO", "HIGH", null, null);
    }

    // --- POST /api/tasks ---

    @Test
    @WithMockUser(username = "peto")
    void createShouldReturn201WithValidBody() throws Exception {
        when(taskService.createTask(eq("peto"), any(CreateTaskRequest.class)))
                .thenReturn(sampleTask);

        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("Új feladat");
        req.setPriority(TaskPriority.HIGH);
        req.setStatus(TaskStatus.TODO);
        req.setStartDate(LocalDateTime.now());

        mockMvc.perform(post("/api/tasks")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Teszt feladat"));

        // The service is called with the authenticated username, not a value from the request body.
        verify(taskService).createTask(eq("peto"), any(CreateTaskRequest.class));
    }

    @Test
    @WithMockUser(username = "peto")
    void createShouldReturn400WhenTitleIsMissing() throws Exception {
        CreateTaskRequest req = new CreateTaskRequest();
        req.setPriority(TaskPriority.MEDIUM);
        req.setStatus(TaskStatus.TODO);
        // title is intentionally missing.

        mockMvc.perform(post("/api/tasks")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(taskService);
    }

    @Test
    @WithMockUser(username = "peto")
    void createShouldReturn400WhenPriorityIsMissing() throws Exception {
        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("Cím");
        req.setStatus(TaskStatus.TODO);
        // priority is intentionally missing.

        mockMvc.perform(post("/api/tasks")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(taskService);
    }

    // --- PUT /api/tasks/{id} ---

    @Test
    @WithMockUser(username = "peto")
    void updateShouldReturn200ForOwner() throws Exception {
        when(taskService.updateTask(eq(1L), eq("peto"), any(CreateTaskRequest.class)))
                .thenReturn(sampleTask);

        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("Módosított cím");
        req.setPriority(TaskPriority.LOW);
        req.setStatus(TaskStatus.DONE);

        mockMvc.perform(put("/api/tasks/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(taskService).updateTask(eq(1L), eq("peto"), any(CreateTaskRequest.class));
    }

    @Test
    @WithMockUser(username = "qwe")
    void updateShouldReturn404WhenNotOwner() throws Exception {
        when(taskService.updateTask(eq(1L), eq("qwe"), any(CreateTaskRequest.class)))
                .thenThrow(new ResourceNotFoundException("Task not found: 1"));

        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("Próbálkozás");
        req.setPriority(TaskPriority.LOW);
        req.setStatus(TaskStatus.TODO);

        mockMvc.perform(put("/api/tasks/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    // --- DELETE /api/tasks/{id} ---

    @Test
    @WithMockUser(username = "peto")
    void deleteShouldReturn204ForOwner() throws Exception {
        doNothing().when(taskService).deleteTask(1L, "peto");

        mockMvc.perform(delete("/api/tasks/1").with(csrf()))
                .andExpect(status().isNoContent());

        verify(taskService).deleteTask(1L, "peto");
    }

    @Test
    @WithMockUser(username = "qwe")
    void deleteShouldReturn404WhenNotOwner() throws Exception {
        doThrow(new ResourceNotFoundException("Task not found: 1"))
                .when(taskService).deleteTask(1L, "qwe");

        mockMvc.perform(delete("/api/tasks/1").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteShouldReturn401WithoutAuthentication() throws Exception {
        mockMvc.perform(delete("/api/tasks/1").with(csrf()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(taskService);
    }
}