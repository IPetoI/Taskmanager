package com.ipetoi.taskmanager.controller;

import com.ipetoi.taskmanager.config.SecurityConfig;
import com.ipetoi.taskmanager.model.Task;
import com.ipetoi.taskmanager.model.TaskPriority;
import com.ipetoi.taskmanager.model.TaskStatus;
import com.ipetoi.taskmanager.model.User;
import com.ipetoi.taskmanager.security.JwtTokenProvider;
import com.ipetoi.taskmanager.service.TaskService;
import com.ipetoi.taskmanager.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import com.ipetoi.taskmanager.TestFixtures;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
class AdminControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    UserService userService;

    @MockBean
    TaskService taskService;

    @MockBean
    JwtTokenProvider jwtTokenProvider;

    private User user;
    private Task task;

    @BeforeEach
    void setUp() {
        user = TestFixtures.regularUser(1L, "peto", "peto@example.com");

        task = TestFixtures.task(7L, "Admin teszt", TaskPriority.HIGH, TaskStatus.TODO,
                LocalDateTime.of(2026, 7, 6, 10, 0), user);
        task.setDescription("Feladat leírás");
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void listUsersShouldReturnOnlyNonAdminUsers() throws Exception {
        when(userService.findAllNonAdminUsers()).thenReturn(List.of(user));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("peto"))
                .andExpect(jsonPath("$[0].email").value("peto@example.com"));

        verify(userService).findAllNonAdminUsers();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void listTasksShouldReturnAllTasks() throws Exception {
        when(taskService.findAllTasks()).thenReturn(List.of(task));

        mockMvc.perform(get("/api/admin/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Admin teszt"))
                .andExpect(jsonPath("$[0].username").value("peto"));

        verify(taskService).findAllTasks();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateUserShouldReturnUpdatedUser() throws Exception {
        User updated = new User();
        updated.setId(1L);
        updated.setUsername("peto2");
        updated.setEmail("peto2@example.com");
        updated.setPassword("Longpassword2.");

        when(userService.updateUser(eq(1L), eq("peto2"), eq("peto2@example.com"), eq("Longpassword2.")))
                .thenReturn(updated);

        mockMvc.perform(put("/api/admin/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"peto2\",\"email\":\"peto2@example.com\",\"password\":\"Longpassword2.\"}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("peto2"))
                .andExpect(jsonPath("$.email").value("peto2@example.com"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void deleteUserShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/admin/users/1").with(csrf()))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(1L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateTaskShouldReturnUpdatedTask() throws Exception {
        Task updated = new Task();
        updated.setId(7L);
        updated.setTitle("Frissített cím");
        updated.setDescription("Feladat leírás");
        updated.setPriority(TaskPriority.LOW);
        updated.setStatus(TaskStatus.DONE);
        updated.setStartDate(LocalDateTime.of(2026, 7, 6, 10, 0));
        updated.setOwner(user);

        when(taskService.adminUpdateTask(eq(7L), any())).thenReturn(updated);

        mockMvc.perform(put("/api/admin/tasks/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Frissített cím\",\"description\":\"Feladat leírás\"," +
                                "\"priority\":\"LOW\",\"status\":\"DONE\",\"startDate\":\"2026-07-06T10:00:00\"}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Frissített cím"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void deleteTaskShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/admin/tasks/7").with(csrf()))
                .andExpect(status().isNoContent());

        verify(taskService).adminDeleteTask(7L);
    }

    @Test
    void listUsersShouldReturn401WithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }
}