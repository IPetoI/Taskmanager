package com.ipetoi.taskmanager.exception;

import com.ipetoi.taskmanager.controller.TaskController;
import com.ipetoi.taskmanager.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(TaskController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;
    @MockBean
    TaskService taskService;

    @Test
    @WithMockUser(username = "peto")
    void resourceNotFoundExceptionShouldReturn404() throws Exception {
        when(taskService.findTasksByUser(anyString(), any(), any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Task not found: 99"));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Task not found: 99"))
                .andExpect(jsonPath("$.path").value("/api/tasks"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser(username = "peto")
    void genericExceptionShouldReturn500WithoutInternalDetails() throws Exception {
        // Internal error details must not be exposed to the client - tests for information leakage.
        String internalMessage = "Database connection refused: jdbc:mysql://localhost:3306/taskmanager";
        when(taskService.findTasksByUser(anyString(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException(internalMessage));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                // Internal message must not be included in the response.
                .andExpect(jsonPath("$.message").value("Unexpected error occurred. Please try again later."))
                // Ensures that the JDBC connection string is not leaked.
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("jdbc"))));
    }

    @Test
    @WithMockUser(username = "peto")
    void illegalArgumentExceptionShouldReturn400() throws Exception {
        when(taskService.findTasksByUser(anyString(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Invalid status value: UNKNOWN"));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid status value: UNKNOWN"));
    }

    @Test
    @WithMockUser(username = "peto")
    void malformedJsonShouldReturn400WithGenericMessage() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"priority\": \"INVALID_ENUM_VALUE\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid request payload"));
    }

    @Test
    @WithMockUser(username = "peto")
    void validationErrorShouldReturn400WithFieldDetails() throws Exception {
        // Empty POST body - Bean Validation returns errors.
        mockMvc.perform(post("/api/tasks")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                // The validation message must contain the name of the invalid field.
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("title")));
    }

    @Test
    @WithMockUser(username = "peto")
    void apiErrorResponseShouldAlwaysHaveTimestamp() throws Exception {
        when(taskService.findTasksByUser(anyString(), any(), any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Task not found: 1"));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }
}