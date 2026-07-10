package com.ipetoi.taskmanager;

import com.ipetoi.taskmanager.dto.LoginRequest;
import com.ipetoi.taskmanager.dto.CreateTaskRequest;
import com.ipetoi.taskmanager.model.TaskPriority;
import com.ipetoi.taskmanager.model.TaskStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


/**
 * Full integration test using the real Spring context, H2 database, and real JWT tokens.
 * Catches issues that unit tests cannot detect (e.g. security configuration,
 * JSON serialization, and JPA mapping problems).
 *
 * @DirtiesContext: resets the application context after each test (clean database state),
 * because persisted data would otherwise affect subsequent tests.
 * @ActiveProfiles("test"): loads application-test.properties (H2 datasource configuration).
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
class TaskManagerIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    // --- Test helper methods ---

    private void registerUser(String username, String password, String email) throws Exception {
        Map<String, String> body = Map.of(
                "username", username,
                "password", password,
                "email", email
        );

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    private Long createTaskAndGetId(String token, String title) throws Exception {
        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle(title);
        req.setPriority(TaskPriority.MEDIUM);
        req.setStatus(TaskStatus.TODO);
        req.setStartDate(LocalDateTime.of(2026, 7, 1, 9, 0));

        MvcResult result = mockMvc.perform(post("/api/tasks")
                        .with(csrf())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("id").asLong();
    }

    // --- Success cases ---

    @Test
    void fullLifecycleShouldWork() throws Exception {
        registerUser("peto", "Jelszo123", "peto@example.com");
        String token = loginAndGetToken("peto", "Jelszo123");

        Long taskId = createTaskAndGetId(token, "Integrációs teszt feladat");

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Integrációs teszt feladat"))
                .andExpect(jsonPath("$[0].username").value("peto"));

        CreateTaskRequest updateReq = new CreateTaskRequest();
        updateReq.setTitle("Módosított cím");
        updateReq.setPriority(TaskPriority.HIGH);
        updateReq.setStatus(TaskStatus.IN_PROGRESS);
        updateReq.setStartDate(LocalDateTime.of(2026, 7, 1, 9, 0));

        mockMvc.perform(put("/api/tasks/" + taskId)
                        .with(csrf())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Módosított cím"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(delete("/api/tasks/" + taskId)
                        .with(csrf())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // --- IDOR regression tests ---

    @Test
    void idor_PetoShouldNotSeeQwetasks() throws Exception {
        registerUser("peto", "Jelszo123", "peto@example.com");
        registerUser("qwe", "qwe", "qwe@example.com");

        String petoToken = loginAndGetToken("peto", "Jelszo123");
        String qweToken = loginAndGetToken("qwe", "qwe");

        createTaskAndGetId(petoToken, "Peto privát feladata");

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", "Bearer " + qweToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void idor_QweShouldNotDeletePetoTask() throws Exception {
        registerUser("peto", "Jelszo123", "peto@example.com");
        registerUser("qwe", "qwe", "qwe@example.com");

        String petoToken = loginAndGetToken("peto", "Jelszo123");
        String qweToken = loginAndGetToken("qwe", "qwe");

        Long PetoTaskId = createTaskAndGetId(petoToken, "Peto feladata");

        // expected response: 404 Not Found.
        mockMvc.perform(delete("/api/tasks/" + PetoTaskId)
                        .with(csrf())
                        .header("Authorization", "Bearer " + qweToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", "Bearer " + petoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void idor_QweShouldNotUpdatePetoTask() throws Exception {
        registerUser("peto", "Jelszo123", "peto@example.com");
        registerUser("qwe", "qwe", "qwe@example.com");

        String petoToken = loginAndGetToken("peto", "Jelszo123");
        String qweToken = loginAndGetToken("qwe", "qwe");

        Long PetoTaskId = createTaskAndGetId(petoToken, "Peto feladata");

        CreateTaskRequest qwesUpdate = new CreateTaskRequest();
        qwesUpdate.setTitle("Qwe felülírta");
        qwesUpdate.setPriority(TaskPriority.HIGH);
        qwesUpdate.setStatus(TaskStatus.DONE);
        qwesUpdate.setStartDate(LocalDateTime.of(2026, 7, 1, 9, 0));

        // expected response: 404 Not Found.
        mockMvc.perform(put("/api/tasks/" + PetoTaskId)
                        .with(csrf())
                        .header("Authorization", "Bearer " + qweToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(qwesUpdate)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", "Bearer " + petoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Peto feladata"))
                .andExpect(jsonPath("$[0].status").value("TODO"));
    }

    // --- Authentication tests ---

    @Test
    void requestWithoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requestWithInvalidTokenShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", "Bearer ez.nem.valodi.token"))
                .andExpect(status().isUnauthorized());
    }

    // --- Filter tests ---

    @Test
    void getTasks_withStatusFilter_returnsOnlyMatchingTasks() throws Exception {
        registerUser("peto", "Jelszo123", "peto@example.com");
        String token = loginAndGetToken("peto", "Jelszo123");

        CreateTaskRequest todoReq = new CreateTaskRequest();
        todoReq.setTitle("TODO feladat");
        todoReq.setPriority(TaskPriority.LOW);
        todoReq.setStatus(TaskStatus.TODO);
        todoReq.setStartDate(LocalDateTime.of(2026, 7, 1, 9, 0));

        CreateTaskRequest doneReq = new CreateTaskRequest();
        doneReq.setTitle("DONE feladat");
        doneReq.setPriority(TaskPriority.LOW);
        doneReq.setStatus(TaskStatus.DONE);
        doneReq.setStartDate(LocalDateTime.of(2026, 7, 1, 9, 0));

        mockMvc.perform(post("/api/tasks").with(csrf())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(todoReq)));

        mockMvc.perform(post("/api/tasks").with(csrf())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(doneReq)));

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", "Bearer " + token)
                        .param("status", "TODO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("TODO feladat"));
    }
}